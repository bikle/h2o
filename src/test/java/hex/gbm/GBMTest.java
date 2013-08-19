package hex.gbm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import org.junit.BeforeClass;
import org.junit.Test;
import water.*;
import water.fvec.*;

public class GBMTest extends TestUtil {

  @BeforeClass public static void stall() { stall_till_cloudsize(2); }

  // ==========================================================================
  /*@Test*/ public void testBasicGBM() {
    File file = TestUtil.find_test_file("./smalldata/logreg/prostate.csv");
    Key fkey = NFSFileVec.make(file);
    Key dest = Key.make("prostate.hex");
    Frame fr = ParseDataset2.parse(dest,new Key[]{fkey});
    UKV.remove(fkey);
    try {
      assertEquals(380,fr._vecs[0].length());

      // Prostate: predict on CAPSULE which is in column #1; move it to last column
      UKV.remove(fr.remove("ID")._key);   // Remove patient ID vector
      Vec capsule = fr.remove("CAPSULE"); // Remove capsule
      fr.add("CAPSULE",capsule);          // Move it to the end

      GBM gbm = GBM.start(GBM.makeKey(),fr,11);
      gbm.get();                  // Block for result
      UKV.remove(gbm.destination_key);
    } finally {
      UKV.remove(dest);
    }
  }

  /*@Test*/ public void testCovtypeGBM() {
    File file = TestUtil.find_test_file("../datasets/UCI/UCI-large/covtype/covtype.data");
    if( file == null ) return;  // Silently abort test if the large covtype is missing
    Key fkey = NFSFileVec.make(file);
    Key dest = Key.make("cov1.hex");
    Frame fr = ParseDataset2.parse(dest,new Key[]{fkey});
    UKV.remove(fkey);
    System.out.println("Parsed into "+fr);
    for( int i=0; i<fr._vecs.length; i++ )
      System.out.println("Vec "+i+" = "+fr._vecs[i]);

    try {
      assertEquals(581012,fr._vecs[0].length());

      // Covtype: predict on last column
      GBM gbm = GBM.start(GBM.makeKey(),fr,30);
      gbm.get();                  // Block for result
      UKV.remove(gbm.destination_key);
    } finally {
      UKV.remove(dest);
    }
  }

  @Test public void testBasicDRF() {
    File file = TestUtil.find_test_file("./smalldata/logreg/prostate.csv");
    Key fkey = NFSFileVec.make(file);
    Key dest = Key.make("prostate.hex");
    Frame fr = ParseDataset2.parse(dest,new Key[]{fkey});
    UKV.remove(fkey);
    try {
      assertEquals(380,fr._vecs[0].length());

      // Prostate: predict on CAPSULE which is in column #1; move it to last column
      UKV.remove(fr.remove("ID")._key);   // Remove patient ID vector
      Vec capsule = fr.remove("CAPSULE"); // Remove capsule
      fr.add("CAPSULE",capsule);          // Move it to the end
      int mtrys = Math.max((int)Math.sqrt(fr.numCols()),1);
      long seed = (1L<<32)|2;

      DRF drf = DRF.start(DRF.makeKey(),fr,/*maxdepth*/50,/*ntrees*/5,mtrys,/*sampleRate*/0.67,seed);
      drf.get();                // Block for result
      UKV.remove(drf.destination_key);

      // Remove the forest model
      Key forestKey = drf._forestKey;
      DRF.Forest forest = UKV.get(forestKey);
      forest.deleteKeys();
      UKV.remove(forestKey);
    } finally {
      UKV.remove(dest);
    }
  }

  @Test public void testCovtypeDRF() {
    File file = TestUtil.find_test_file("../datasets/UCI/UCI-large/covtype/covtype.data");
    if( file == null ) return;  // Silently abort test if the large covtype is missing
    Key fkey = NFSFileVec.make(file);
    Key dest = Key.make("cov1.hex");
    Frame fr = ParseDataset2.parse(dest,new Key[]{fkey});
    UKV.remove(fkey);
    System.out.println("Parsed into "+fr);
    for( int i=0; i<fr._vecs.length; i++ )
      System.out.println("Vec "+i+" = "+fr._vecs[i]);

    try {
      assertEquals(581012,fr._vecs[0].length());

      // Confirm that on a multi-JVM test setup, covtype gets spread around
      Vec c0 = fr.firstReadable();
      int N = c0.nChunks();
      H2ONode h2o =  c0.chunkKey(0).home_node(); // A chunkkey home
      boolean found=false;        // Found another chunkkey home?
      for( int j=1; j<N; j++ )    // All the chunks
        if( h2o != c0.chunkKey(j).home_node() ) found = true;
      assertTrue("Expecting to find distribution",found || H2O.CLOUD.size()==1);

      // Covtype: predict on last column
      int mtrys = Math.max((int)Math.sqrt(fr.numCols()),1);
      long seed = (1L<<32)|2;

      DRF drf = DRF.start(DRF.makeKey(),fr,/*maxdepth*/50,/*ntrees*/5,mtrys,/*sampleRate*/0.67,seed);
      drf.get();                  // Block for result
      UKV.remove(drf.destination_key);

      // Remove the forest model
      Key forestKey = drf._forestKey;
      DRF.Forest forest = UKV.get(forestKey);
      forest.deleteKeys();
      UKV.remove(forestKey);

    } finally {
      UKV.remove(dest);
    }
  }
}
