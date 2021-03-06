package independentVariable;

import dependentVariable.RTMeasure;
import logrecorder.RTLog;
import mutantSet.BinSet;
import mutantSet.MutantSet;
import mutantSet.TestMethods;
import testcases.Bean;
import testcases.GenerateTestcases;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author phantom
 */
public class RT {
    private static final int SEEDS = 30;
    private static final int TESTTIMES = 30;
    private static final double DIVISOR = SEEDS * TESTTIMES;
    private static final int NUMOFTESTCASES = 900000;
    private static final String ORIGINAL_PACKAGE = "com.lyq.";
    public void randomTesting(){
        GenerateTestcases generateTestcases = new GenerateTestcases();

        TestMethods testMethods = new TestMethods();
        List<String> methodsList = testMethods.getMethods();
        //        int[] partitions = {40,35,20,14,10};//记录每一个分区之中变异体的数量
        int[] partitions = {11};
//        String[] distribution = {"M50-50","M60-40","M70-30","M80-20","M90-10"};
        String[] distribution = {"LowFailureRate"};
        for (int y = 0; y < distribution.length; y++) {
            //记录每一个测试序列的测试结果
            RTMeasure rtMeasure = new RTMeasure();
            RTLog rtLog = new RTLog();

            List<Long> falltime = new ArrayList<Long>();

            List<Long> f2alltime = new ArrayList<Long>();

            List<Long> talltime = new ArrayList<Long>();

            for (int i = 0; i < SEEDS; i++) {
                for (int r = 0; r < TESTTIMES; r++) {
                    //获得变异体集
                    MutantSet ms = new MutantSet();
                    BinSet[] mutants = new BinSet[5];
                    for (int j = 0; j < mutants.length; j++) {
                        mutants[j] = new BinSet();
                    }
                    mutants = ms.getMutantsList();
                    List<Bean> beans = new ArrayList<Bean>();
                    //产生测试用例
                    beans.clear();
                    beans = generateTestcases.generateTestcases(i,NUMOFTESTCASES);
                    //记录被杀死的变异体
                    List<String> killedMutants = new ArrayList<String>();
                    killedMutants.clear();
                    int counter = 0 ;
                    int fmeasure = 0 ;
                    long starttemp = System.currentTimeMillis();
                    long ftime = 0;


                    for (int j = 0; j < beans.size(); j++) {//每一个测试用例要在所有的变异体上执行
                        System.out.println("test begin:");
                        Bean bean = beans.get(j);//当前测试用例
                        counter++;
                        //记录临时某一个测试用例杀死的变异体情况
                        List<String> templist = new ArrayList<String>();
                        templist.clear();
                        try{
                            //开始逐个遍历变异体
                            for (int k = 0; k < mutants[y].size(); k++) {
                                //获取原始程序的实例
                                Class originalClazz = Class.forName(ORIGINAL_PACKAGE+"ChinaUnionBill");
                                Constructor constructor1 = originalClazz.getConstructor(null);
                                Object originalInstance = constructor1.newInstance(null);
                                //获取变异体程序的实例
                                Class mutantClazz = Class.forName(mutants[y].getMutantName(k));
                                Constructor constructor2 = mutantClazz.getConstructor(null);
                                Object mutantInstance = constructor2.newInstance(null);
                                //对一个变异体的所有方法进行遍历
                                for (int l = 0; l < methodsList.size(); l++) {
                                    //获取源程序的方法
                                    Method originalMethod = originalClazz.getMethod(methodsList.get(l),char.class,int.class,double.class,int.class,int.class);
                                    Object originalResult =  originalMethod.invoke(originalInstance,bean.getType(),bean.getMonthRent(),bean.getFlow(),bean.getVoiceCall(),bean.getVideoCall());

                                    Method mutantMethod = mutantClazz.getMethod(methodsList.get(l),char.class,int.class,double.class,int.class,int.class);
                                    Object mutantResult = mutantMethod.invoke(mutantInstance,bean.getType(),bean.getMonthRent(),bean.getFlow(),bean.getVoiceCall(),bean.getVideoCall());
//
                                    if (!originalResult.equals(mutantResult)){//揭示故障
//                                    System.out.println("original"+originalResult + "mutant"+mutantResult);
                                        String[] str = mutants[y].getMutantName(k).split("\\.");
                                        //删除杀死的变异体
                                        mutants[y].remove(k);
                                        k--;
                                        String temp = str[3];
                                        killedMutants.add(temp);
                                        templist.add(temp);
                                        if (killedMutants.size() == 1){
                                            fmeasure = counter;
                                            rtMeasure.addFmeasure(counter);
                                            long ftimeTemp = System.currentTimeMillis();
                                            ftime = ftimeTemp;
                                            falltime.add(ftimeTemp - starttemp);
                                        }else if (killedMutants.size() == partitions[y]){
                                            rtMeasure.addTmeasure(counter);
                                            long ttimeTemp = System.currentTimeMillis();
                                            talltime.add(ttimeTemp - starttemp);
                                        }else if (killedMutants.size() == 2){
                                            rtMeasure.addNFmeasure(counter - fmeasure);
                                            long f2timeTemp = System.currentTimeMillis();
                                            f2alltime.add(f2timeTemp - ftime);
                                        }
                                        break;
                                    }
                                }
                            }
                            //记录1个测试用例在所有得变异体上执行之后的结果
//                        rtLog.recordProcessInfo("RT_log.txt",distribution[y],String.valueOf(i),
//                                String.valueOf(bean.getId()),
//                                templist,String.valueOf(partitions[y] - killedMutants.size()));
                            if (killedMutants.size() >= partitions[y]){
                                break;
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            DecimalFormat decimalFormat = new DecimalFormat("#.00");
            long ftotaltime = 0;
            for (int j = 0; j < falltime.size(); j++) {
                ftotaltime += falltime.get(j);
            }
            double meanfTime = Double.parseDouble(decimalFormat.format(ftotaltime / DIVISOR));

            long f2totaltime = 0;
            for (int j = 0; j < f2alltime.size(); j++) {
                f2totaltime += f2alltime.get(j);
            }
            double meanf2time = Double.parseDouble(decimalFormat.format(f2totaltime / DIVISOR));

            long ttotaltime = 0 ;

            for (int j = 0; j < talltime.size(); j++) {
                ttotaltime += talltime.get(j);
            }
            double meantime = Double.parseDouble(decimalFormat.format(ttotaltime / DIVISOR)) ;

            rtLog.recordResult("RTResult.txt",distribution[y],rtMeasure.getMeanFmeasure(),
                    rtMeasure.getMeanNFmeasure(),rtMeasure.getMeanTmeasure(),rtMeasure.getStandardDevOfFmeasure(),
                    rtMeasure.getMeanNFmeasure(),rtMeasure.getStandardDevOfTmeasure(),meanfTime,meanf2time,meantime);
        }
    }
    public static void main(String[] args) {
        RT rt = new RT();
        rt.randomTesting();
    }

}
