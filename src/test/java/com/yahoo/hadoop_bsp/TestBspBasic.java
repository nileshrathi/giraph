package com.yahoo.hadoop_bsp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import java.util.List;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.yahoo.hadoop_bsp.examples.GeneratedVertexInputFormat;
import com.yahoo.hadoop_bsp.examples.GeneratedVertexReader;
import com.yahoo.hadoop_bsp.examples.SimpleCombinerVertex;
import com.yahoo.hadoop_bsp.examples.SimpleFailVertex;
import com.yahoo.hadoop_bsp.examples.SimpleMsgVertex;
import com.yahoo.hadoop_bsp.examples.SimplePageRankVertex;
import com.yahoo.hadoop_bsp.examples.SimpleSumCombiner;
import com.yahoo.hadoop_bsp.examples.SimpleSuperstepVertex;
import com.yahoo.hadoop_bsp.examples.SimpleVertexWriter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit test for many simple BSP applications.
 */
public class TestBspBasic extends BspCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestBspBasic(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestBspBasic.class);
    }

    /**
     * Just instantiate the vertex (all functions are implemented) and the
     * VertexInputFormat using reflection.
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InterruptedException
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    public void testInstantiateVertex()
        throws InstantiationException, IllegalAccessException,
        IOException, InterruptedException, IllegalArgumentException,
        InvocationTargetException, SecurityException, NoSuchMethodException {
        System.out.println("testInstantiateVertex: java.class.path=" +
                           System.getProperty("java.class.path"));
        java.lang.reflect.Constructor<?> ctor =
            SimpleSuperstepVertex.class.getConstructor();
        assertNotNull(ctor);
        SimpleSuperstepVertex test =
            (SimpleSuperstepVertex) ctor.newInstance();
        System.out.println("testInstantiateVertex: superstep=" +
                           test.getSuperstep());
        GeneratedVertexInputFormat inputFormat =
            GeneratedVertexInputFormat.class.newInstance();
        Configuration conf = new Configuration();
        List<InputSplit> splitArray = inputFormat.getSplits(conf, 1);
        ByteArrayOutputStream byteArrayOutputStream =
            new ByteArrayOutputStream();
        DataOutputStream outputStream =
            new DataOutputStream(byteArrayOutputStream);
        ((Writable) splitArray.get(0)).write(outputStream);
        System.out.println("testInstantiateVertex: Example output split = " +
                           byteArrayOutputStream.toString());
    }

    /**
     * Run a sample BSP job in JobTracker, kill a task, and make sure the job fails.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void testBspFail()
            throws IOException, InterruptedException, ClassNotFoundException {
        // Allow this test only to be run on a real Hadoop setup
        if (getJobTracker() == null) {
            System.out.println("testBspFail: not executed for local setup.");
            return;
        }
        Configuration conf = new Configuration();
        setupConfiguration(conf);
        conf.setInt("mapred.map.max.attempts", 2);
        FileSystem hdfs = FileSystem.get(conf);
        conf.setClass(BspJob.BSP_VERTEX_CLASS,
                      SimpleFailVertex.class,
                      HadoopVertex.class);
        conf.setClass(BspJob.BSP_VERTEX_INPUT_FORMAT_CLASS,
                      GeneratedVertexInputFormat.class,
                      VertexInputFormat.class);
        BspJob bspJob = new BspJob(conf, "testBspFail");
        Path outputPath = new Path("/tmp/testBspFailOutput");
        hdfs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(bspJob, outputPath);
        assertTrue(!bspJob.run(true));
    }

    /**
     * Run a sample BSP job locally and test supersteps.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void testBspSuperStep()
        throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        setupConfiguration(conf);
        conf.setFloat(BspJob.BSP_TOTAL_INPUT_SPLIT_MULTIPLIER, 2.0f);
        // GeneratedInputSplit will generate 10 vertices
        conf.setLong(GeneratedVertexReader.READER_VERTICES, 10);
        FileSystem hdfs = FileSystem.get(conf);
        conf.setClass(BspJob.BSP_VERTEX_CLASS,
                      SimpleSuperstepVertex.class,
                      HadoopVertex.class);
        conf.setClass(BspJob.BSP_VERTEX_INPUT_FORMAT_CLASS,
                      GeneratedVertexInputFormat.class,
                      VertexInputFormat.class);
        conf.setClass(BspJob.BSP_VERTEX_WRITER_CLASS,
                      SimpleVertexWriter.class,
                      VertexWriter.class);
        BspJob bspJob = new BspJob(conf, "testBspSuperStep");
        Path outputPath = new Path("/tmp/testBspSuperStepOutput");
        hdfs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(bspJob, outputPath);
        assertTrue(bspJob.run(true));
        if (getJobTracker() == null) {
            FileStatus fileStatus = getSinglePartFileStatus(hdfs, outputPath);
            assertTrue(fileStatus.getLen() == 49);
        }
    }

    /**
     * Run a sample BSP job locally and test messages.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void testBspMsg()
        throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        conf.set("mapred.jar", getJarLocation());
        setupConfiguration(conf);
        FileSystem hdfs = FileSystem.get(conf);
        conf.setClass(BspJob.BSP_VERTEX_CLASS,
                      SimpleMsgVertex.class,
                      HadoopVertex.class);
        conf.setClass(BspJob.BSP_VERTEX_INPUT_FORMAT_CLASS,
                      GeneratedVertexInputFormat.class,
                      VertexInputFormat.class);
        BspJob bspJob = new BspJob(conf, "testBspMsg");
        Path outputPath = new Path("/tmp/testBspMsgOutput");
        hdfs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(bspJob, outputPath);
        assertTrue(bspJob.run(true));
    }


    /**
     * Run a sample BSP job locally with no vertices and make sure
     * it completes.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void testEmptyVertexInputFormat()
        throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        setupConfiguration(conf);
        FileSystem hdfs = FileSystem.get(conf);
        conf.setClass(BspJob.BSP_VERTEX_CLASS,
                      SimpleMsgVertex.class,
                      HadoopVertex.class);
        conf.setClass(BspJob.BSP_VERTEX_INPUT_FORMAT_CLASS,
                      GeneratedVertexInputFormat.class,
                      VertexInputFormat.class);
        conf.setLong(GeneratedVertexReader.READER_VERTICES, 0);
        BspJob bspJob = new BspJob(conf, "testEmptyVertexInputFormat");
        Path outputPath = new Path("/tmp/testEmptyVertexInputFormat");
        hdfs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(bspJob, outputPath);
        assertTrue(bspJob.run(true));
    }

    /**
     * Run a sample BSP job locally with combiner and checkout output value.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void testBspCombiner()
        throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        setupConfiguration(conf);
        FileSystem hdfs = FileSystem.get(conf);
        conf.setClass(BspJob.BSP_VERTEX_CLASS,
                      SimpleCombinerVertex.class,
                      HadoopVertex.class);
        conf.setClass(BspJob.BSP_VERTEX_INPUT_FORMAT_CLASS,
                      GeneratedVertexInputFormat.class,
                      VertexInputFormat.class);
        conf.setClass(BspJob.BSP_COMBINER_CLASS,
                      SimpleSumCombiner.class,
                      Combiner.class);
        BspJob bspJob = new BspJob(conf, "testBspCombiner");
        Path outputPath = new Path("/tmp/testBspCombinerOutput");
        hdfs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(bspJob, outputPath);
        assertTrue(bspJob.run(true));
    }

    /**
     * Run a sample BSP job locally and test PageRank.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void testBspPageRank()
        throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        setupConfiguration(conf);
        FileSystem hdfs = FileSystem.get(conf);
        conf.setClass(BspJob.BSP_VERTEX_CLASS,
                      SimplePageRankVertex.class,
                      HadoopVertex.class);
        conf.setClass(BspJob.BSP_VERTEX_INPUT_FORMAT_CLASS,
                      GeneratedVertexInputFormat.class,
                      VertexInputFormat.class);
        BspJob bspJob = new BspJob(conf, "testBspPageRank");
        Path outputPath = new Path("/tmp/testBspPageRankOutput");
        hdfs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(bspJob, outputPath);
        assertTrue(bspJob.run(true));
        if (getJobTracker() == null) {
            double maxPageRank = SimplePageRankVertex.finalMax;
            double minPageRank = SimplePageRankVertex.finalMin;
            long numVertices = SimplePageRankVertex.finalSum;
            System.out.println("testBspPageRank: maxPageRank=" + maxPageRank +
                               " minPageRank=" + minPageRank +
                               " numVertices=" + numVertices);
            assertTrue(maxPageRank > 0.19847 && maxPageRank < 0.19848);
            assertTrue(minPageRank > 0.03 && minPageRank < 0.03001);
            assertTrue(numVertices == 5);
        }
    }
}