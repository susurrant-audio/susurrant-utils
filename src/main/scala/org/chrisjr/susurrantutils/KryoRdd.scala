package org.chrisjr.susurrantutils
import java.io._

import com.esotericsoftware.kryo.io.Input
import org.apache.hadoop.io.{BytesWritable, NullWritable}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.{SparkConf, SparkContext}

import scala.reflect.ClassTag

object KryoRdd {
  //https://github.com/phatak-dev/blog/blob/master/code/kryoexample/src/main/scala/com/madhu/spark/kryo/KryoExample.scala
  
  def saveOne[T](iter: Iterator[T], path: String, length: Int = Int.MaxValue) {
      val conf = new SparkConf()
      val kryoSerializer = new KryoSerializer(conf)
          //initializes kryo and calls your registrator class
      val kryo = kryoSerializer.newKryo()

      //convert data to bytes
      val fos = new FileOutputStream(path)
      val buffered = new BufferedOutputStream(fos)
      val output = kryoSerializer.newKryoOutput()
      output.setOutputStream(buffered)
      for ((t, i) <- iter.zipWithIndex) {
        kryo.writeClassAndObject(output, t)
        if (i % 1024 == 0 && length < Int.MaxValue) {
          println(s"${i * 100.0/length}%")
        }
      }
      output.close()
  }

  def saveAsObjectFile[T: ClassTag](rdd: RDD[T], path: String) {
    val kryoSerializer = new KryoSerializer(rdd.context.getConf)

    rdd.mapPartitions(iter => iter.grouped(10)
      .map(_.toArray))
      .map(splitArray => {
      //initializes kryo and calls your registrator class
      val kryo = kryoSerializer.newKryo()

      //convert data to bytes
      val bao = new ByteArrayOutputStream()
      val output = kryoSerializer.newKryoOutput()
      output.setOutputStream(bao)
      kryo.writeClassAndObject(output, splitArray)
      output.close()

      // We are ignoring key field of sequence file
      val byteWritable = new BytesWritable(bao.toByteArray)
      (NullWritable.get(), byteWritable)
    }).saveAsSequenceFile(path)
  }
  def objectFile[T](sc: SparkContext, path: String, minPartitions: Int = 1)(implicit ct: ClassTag[T]) = {
    val kryoSerializer = new KryoSerializer(sc.getConf)
    sc.sequenceFile(path, classOf[NullWritable], classOf[BytesWritable],
       minPartitions)
       .flatMap(x => {
       val kryo = kryoSerializer.newKryo()
       val input = new Input()
       input.setBuffer(x._2.getBytes)
       val data = kryo.readClassAndObject(input)
       val dataObject = data.asInstanceOf[Array[T]]
       dataObject
    })
  }
}