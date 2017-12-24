package com.spare.cointrade;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.spare.cointrade.model.depth.HuobiDepth;
import com.spare.cointrade.model.OkCoinData;
import com.spare.cointrade.realtime.okcoin.model.OkcoinDepth;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by dada on 2017/8/20.
 */
public class TestUtil {

    @Test
    public void deserial() {
        String huobiDepth = "{\"ch\":\"market.btccny.depth.step0\",\"ts\":1503217990897,\"tick\":{\"id\":1503217989979,\"ts\":1503217989979,\"bids\":[[28150.230000,3.240000],[28150.200000,0.089300],[28150.000000,2.399100],[28133.100000,0.004900],[28118.040000,0.760000],[28118.010000,0.340400],[28118.000000,0.071400],[28100.000000,0.100000],[28088.090000,0.170000],[28088.000000,0.389700],[28080.220000,0.151100],[28080.000000,0.311200],[28060.000000,0.461300],[28050.000000,0.075900],[28042.890000,0.110000],[28022.910000,2.300000],[28020.160000,1.890000],[28020.130000,0.890000],[28020.100000,0.050000],[28020.040000,0.710000],[28020.010000,0.300000],[28020.000000,0.510000],[28015.070000,0.096000],[28003.640000,0.500000],[28001.000000,0.010000],[28000.100000,0.106000],[28000.000000,5.027600],[27977.570000,2.800000],[27975.500000,0.002900],[27964.970000,0.500000],[27960.000000,0.848200],[27950.000000,0.500000],[27938.070000,0.070000],[27931.380000,3.000000],[27915.810000,0.273000],[27908.000000,0.056200],[27907.000000,0.287900],[27905.000000,0.010000],[27901.780000,0.014000],[27901.030000,0.020000],[27901.010000,0.450000],[27900.100000,0.566700],[27900.060000,0.010000],[27900.030000,0.960000],[27900.000000,13.391800],[27888.130000,0.500000],[27888.100000,1.797900],[27887.000000,0.122800],[27875.410000,3.000000],[27871.000000,0.074300],[27867.000000,0.625000],[27860.520000,0.500000],[27858.260000,0.009500],[27858.250000,0.080000],[27858.000000,0.097900],[27857.720000,0.273000],[27855.660000,0.400000],[27855.410000,6.000000],[27850.000000,0.535200],[27847.910000,0.010000],[27847.660000,0.150000],[27847.000000,0.252200],[27837.660000,0.300000],[27837.000000,0.217600],[27828.000000,0.119600],[27827.630000,0.001000],[27820.000000,0.001100],[27815.660000,0.600000],[27811.030000,0.500000],[27811.000000,0.074900],[27810.000000,0.337900],[27808.000000,0.100000],[27806.410000,0.100000],[27802.010000,0.050000],[27801.100000,2.000000],[27801.000000,0.186200],[27800.980000,7.514700],[27800.500000,0.071900],[27800.450000,0.107900],[27800.100000,0.170500],[27800.010000,0.719300],[27800.000000,12.206700],[27788.000000,0.592500],[27780.960000,0.371000],[27780.000000,0.010000],[27777.000000,5.000000],[27770.000000,0.025000],[27758.530000,0.200000],[27752.000000,0.120000],[27750.000000,1.601700],[27743.340000,0.500000],[27743.310000,0.103400],[27740.000000,1.200000],[27737.000000,1.307400],[27730.000000,1.300000],[27720.000000,1.493000],[27718.010000,0.100000],[27716.000000,0.100000],[27710.000000,1.400000],[27708.000000,0.100000],[27701.000000,0.372100],[27700.040000,0.500000],[27700.010000,6.046000],[27700.000000,9.557200],[27690.000000,1.400000],[27680.030000,0.500000],[27680.000000,1.669300],[27670.000000,1.600000],[27665.020000,0.100000],[27660.000000,1.600000],[27658.000000,0.100000],[27651.000000,0.761800],[27650.030000,0.500000],[27650.000000,14.346400],[27640.000000,2.878800],[27630.000000,1.411400],[27625.410000,1.263600],[27622.200000,0.039900],[27620.000000,1.246400],[27615.000000,0.501500],[27611.000000,0.036200],[27610.000000,2.585000],[27608.690000,0.100000],[27601.000000,1.140600],[27600.130000,1.000000],[27600.010000,6.046000],[27600.000000,4.285700],[27599.000000,0.022000],[27590.000000,0.703200],[27586.080000,0.100000],[27585.000000,0.054700],[27583.840000,0.200000],[27580.000000,0.872700],[27570.000000,1.128900],[27569.850000,0.200000],[27568.000000,0.100000],[27562.300000,0.036200],[27560.130000,0.379000],[27560.000000,2.005500],[27559.000000,2.035300],[27555.000000,0.018100],[27554.870000,0.001000],[27550.000000,1.121600],[27542.030000,0.500000],[27542.000000,0.044800],[27540.000000,1.800000],[27530.750000,0.238000],[27528.000000,0.107900],[27521.300000,0.283900],[27520.000000,5.249300]],\"asks\":[[28189.400000,0.105100],[28190.000000,0.037700],[28198.290000,0.335400],[28198.990000,0.018100],[28199.000000,0.843300],[28199.630000,0.600000],[28199.650000,0.813100],[28200.000000,1.103800],[28210.940000,0.018700],[28212.000000,0.703200],[28213.000000,0.998000],[28213.190000,0.019300],[28216.750000,0.018700],[28218.980000,1.480000],[28220.000000,0.312200],[28222.000000,0.015400],[28225.130000,0.381400],[28228.000000,0.250500],[28229.240000,0.490000],[28229.270000,2.800000],[28229.900000,0.099800],[28230.000000,0.510600],[28240.000000,0.040400],[28245.000000,0.081700],[28245.520000,0.018700],[28248.750000,0.490000],[28249.970000,0.220000],[28250.000000,1.386200],[28259.000000,0.068900],[28259.870000,0.500000],[28259.900000,1.000000],[28260.000000,3.546900],[28265.000000,0.003600],[28267.000000,0.008200],[28268.000000,0.128100],[28270.000000,0.463500],[28276.000000,0.002000],[28277.000000,0.055900],[28279.950000,0.595900],[28279.970000,0.500000],[28280.000000,2.358500],[28283.000000,3.229600],[28288.000000,0.689900],[28288.050000,0.200000],[28290.000000,0.081700],[28292.000000,0.139400],[28295.000000,1.000000],[28295.080000,0.410900],[28296.540000,0.014500],[28297.970000,0.900000],[28298.000000,8.738300],[28299.000000,2.421600],[28299.990000,4.675700],[28300.000000,15.210100],[28300.040000,0.279500],[28300.880000,0.800000],[28311.010000,0.249800],[28313.670000,0.215000],[28314.980000,0.465700],[28318.000000,4.889200],[28319.710000,0.033100],[28324.930000,0.433200],[28325.000000,0.191700],[28328.450000,0.300000],[28329.970000,0.500000],[28330.000000,0.519500],[28333.000000,0.498200],[28333.100000,0.176900],[28335.000000,0.039900],[28337.000000,0.005800],[28338.470000,0.229200],[28340.000000,0.107200],[28345.990000,0.487600],[28348.000000,0.100000],[28350.000000,1.627000],[28350.300000,4.720000],[28352.000000,0.050000],[28357.410000,0.018700],[28359.980000,1.000000],[28360.000000,0.289500],[28365.000000,0.002000],[28366.000000,0.185400],[28368.000000,0.059200],[28370.010000,0.215000],[28377.000000,0.010000],[28380.000000,3.950000],[28386.970000,0.001000],[28387.970000,0.500000],[28388.000000,0.303200],[28389.000000,0.050100],[28398.000000,5.100000],[28399.000000,6.994700],[28400.000000,9.264200],[28400.970000,1.342200],[28404.320000,0.500000],[28406.700000,0.243900],[28409.610000,0.150000],[28419.590000,0.003000],[28419.610000,0.300000],[28426.000000,0.005000],[28430.000000,1.200000],[28436.000000,0.749000],[28440.000000,0.022200],[28444.290000,0.500000],[28444.320000,0.750000],[28448.000000,0.100000],[28449.350000,0.419000],[28450.000000,2.874700],[28451.750000,0.102100],[28455.000000,0.200000],[28459.000000,3.528700],[28460.000000,0.838400],[28465.000000,2.500000],[28468.000000,0.300000],[28470.000000,0.035000],[28475.000000,0.120000],[28475.500000,0.171500],[28477.100000,1.700000],[28479.220000,0.355500],[28480.000000,0.250000],[28485.770000,1.419800],[28485.880000,0.814100],[28487.880000,0.076900],[28488.390000,0.180900],[28490.000000,0.217500],[28498.000000,0.617900],[28498.500000,0.012600],[28498.660000,0.123900],[28498.980000,7.408500],[28499.000000,32.803700],[28499.500000,0.866900],[28499.900000,2.000000],[28499.970000,0.200000],[28499.990000,6.546000],[28500.000000,54.244600],[28500.120000,0.299400],[28506.100000,0.090900],[28508.000000,0.099800],[28509.090000,0.034000],[28510.000000,0.070000],[28512.000000,0.003900],[28523.000000,0.331800],[28530.000000,0.020000],[28535.000000,0.026800],[28538.000000,0.057500],[28549.970000,0.500000],[28550.000000,2.052000],[28554.000000,0.998100],[28555.170000,0.030000],[28556.000000,0.099800]]}}";

        HuobiDepth depth = JSON.parseObject(huobiDepth, HuobiDepth.class);


        System.out.println(depth);
    }

    @Test
    public void deserialOkCoin() {
        String data = "[{\"data\":{\"asks\":[[\"4373.54\",\"0.025\"],[\"4416.7\",\"0\"]],\"bids\":[],\"timestamp\":1503226837137},\"channel\":\"ok_sub_spot_btc_depth\"}]";
        Type type = new TypeReference<List<OkCoinData>>() {}.getType();
        List<OkCoinData> okCoinDataList = JSON.parseObject(data, type);
        System.out.println(okCoinDataList);

        if(okCoinDataList.get(0).getData() instanceof JSONObject) {
            OkcoinDepth depth = ((JSONObject) okCoinDataList.get(0).getData()).toJavaObject(OkcoinDepth.class);

            System.out.println(depth);

        }
    }

}
