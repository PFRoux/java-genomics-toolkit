package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Take the nth-root of a (Big)Wig file
 * 
 * @author timpalpant
 *
 */
public class Root extends WigMathTool {

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-n", "--root" }, description = "Take the nth root (default = 2)")
  public double root = 2;

  WigFileReader reader;
  private double baseChange;

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);
    } catch (IOException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(reader);
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    float[] result = reader.query(chunk).getValues();
    for (int i = 0; i < result.length; i++) {
      result[i] = (float) (Math.sqrt(result[i]));
    }

    return result;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new Root().instanceMain(args);
  }

}
