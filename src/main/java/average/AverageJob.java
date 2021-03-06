package average;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class AverageJob extends Configured implements Tool {

	public enum Counter {
		MAP, COMBINER, REDUCE
	}

	public static class AverageMapper extends
			Mapper<LongWritable, Text, Text, Text> {

		private Text outputKey = new Text();
		private Text outputValue = new Text();
		private static final String ONE = ",1";

		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String[] words = StringUtils.split(value.toString(), '\\', ',');
			outputKey.set(words[1].trim().toString());
			outputValue.set(words[9] + ONE);
			context.write(outputKey, outputValue);
			context.getCounter(Counter.MAP).increment(1);
		}

		@Override
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
		}
	}

	public static class AverageCombiner extends Reducer<Text, Text, Text, Text> {
		private Text outputValue = new Text();
		private String COMMA = ",";

		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			long sum = 0;
			int count = 0;
			while (values.iterator().hasNext()) {
				String current = values.iterator().next().toString();
				String[] words = StringUtils.split(current.toString(), '\\',
						',');
				sum += Long.parseLong(words[0]);
				count += Integer.parseInt(words[1]);
			}
			outputValue.set(sum + COMMA + count);
			context.write(key, outputValue);
			context.getCounter(Counter.COMBINER).increment(1);
		}

		@Override
		protected void cleanup(Context context) throws IOException,
				InterruptedException {

		}
	}

	public static class AverageReducer extends
			Reducer<Text, Text, Text, DoubleWritable> {
		private DoubleWritable outputValue = new DoubleWritable();

		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			long sum = 0;
			int count = 1;
			while (values.iterator().hasNext()) {
				String current = values.iterator().next().toString();
				String[] words = StringUtils.split(current.toString(), '\\',
						',');
				sum += Long.parseLong(words[0]);
				count += Integer.parseInt(words[1]);
			}
			outputValue.set(((double) sum) / count);
			context.write(key, outputValue);
			context.getCounter(Counter.REDUCE).increment(1);
		}

		@Override
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
		}
	}

	@Override
	public int run(String[] arg0) throws Exception {
		Configuration conf = super.getConf();
		Job job = Job.getInstance(conf, "AverageJob");
		job.setJarByClass(AverageJob.class);

		Path out = new Path("output");
		FileInputFormat.setInputPaths(job, "counties");
		FileOutputFormat.setOutputPath(job, out);
		out.getFileSystem(conf).delete(out, true);

		job.setMapperClass(AverageMapper.class);
		job.setReducerClass(AverageReducer.class);
		job.setCombinerClass(AverageCombiner.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) {
		int result = 0;
		try {
			result = ToolRunner
					.run(new Configuration(), new AverageJob(), args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(result);
	}
}