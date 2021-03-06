package org.learningu.scheduling.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;

import java.util.Map;

import org.learningu.scheduling.Pass.OptimizerSpec;
import org.learningu.scheduling.Pass.SerialAcceptanceFunction;
import org.learningu.scheduling.Pass.SerialTemperatureFunction;
import org.learningu.scheduling.annotations.SingleThread;
import org.learningu.scheduling.optimization.AcceptanceFunction;
import org.learningu.scheduling.optimization.Annealer;
import org.learningu.scheduling.optimization.ConcurrentOptimizer;
import org.learningu.scheduling.optimization.Optimizer;
import org.learningu.scheduling.optimization.OptimizerFactory;
import org.learningu.scheduling.optimization.Perturber;
import org.learningu.scheduling.optimization.StandardAcceptanceFunction;
import org.learningu.scheduling.optimization.TemperatureFunction;
import org.learningu.scheduling.perturbers.Perturbers;
import org.learningu.scheduling.schedule.Schedule;
import org.learningu.scheduling.scorers.SerialScorers.CompleteScorer;

public class OptimizerModule extends AbstractModule {

  @Override
  protected void configure() {
    MapBinder<SerialAcceptanceFunction, AcceptanceFunction> acceptBinder = MapBinder.newMapBinder(
        binder(),
        SerialAcceptanceFunction.class,
        AcceptanceFunction.class);
    acceptBinder.addBinding(SerialAcceptanceFunction.STANDARD_EXPONENTIAL).to(
        StandardAcceptanceFunction.class);
    MapBinder<SerialTemperatureFunction, TemperatureFunction> tempBinder = MapBinder.newMapBinder(
        binder(),
        SerialTemperatureFunction.class,
        TemperatureFunction.class);
    tempBinder.addBinding(SerialTemperatureFunction.LINEAR).toInstance(PredefinedTemperatureFunctions.LINEAR_FUNCTION);
    tempBinder.addBinding(SerialTemperatureFunction.QUADRATIC).toInstance(PredefinedTemperatureFunctions.QUADRATIC_FUNCTION);
    tempBinder.addBinding(SerialTemperatureFunction.FLAT).toInstance(PredefinedTemperatureFunctions.FLAT);
    install(new FactoryModuleBuilder().implement(
        new TypeLiteral<Optimizer<Schedule>>() {},
        new TypeLiteral<Annealer<Schedule>>() {}).build(
        Key.get(new TypeLiteral<OptimizerFactory<Schedule>>() {}, SingleThread.class)));
    install(new ScorerModule());
  }

  @Provides
  Optimizer<Schedule> optimizer(ConcurrentOptimizer<Schedule> opt) {
    return opt;
  }

  @Provides
  CompleteScorer completeScorer(OptimizerSpec spec) {
    return spec.getScorer();
  }

  @Provides
  Perturber<Schedule> perturber(OptimizerSpec spec) {
    return Perturbers.deserialize(spec.getPerturber());
  }

  @Provides
  @Named("primaryTempFun")
  TemperatureFunction primaryTemperatureFunction(OptimizerSpec spec,
      Map<SerialTemperatureFunction, Provider<TemperatureFunction>> bindings) {
    return bindings.get(spec.getPrimaryTempFun()).get();
  }

  @Provides
  @Named("subTempFun")
  TemperatureFunction subTemperatureFunction(OptimizerSpec spec,
      Map<SerialTemperatureFunction, Provider<TemperatureFunction>> bindings) {
    return bindings.get(spec.getSubTempFun()).get();
  }

  @Provides
  @Named("subOptimizerSteps")
  int subOptimizerSteps(OptimizerSpec spec) {
    return spec.getSubOptimizerSteps();
  }

  @Provides
  @Named("nSubOptimizers")
  int nSubOptimizers(OptimizerSpec spec) {
    return spec.getNumberOfSubOptimizers();
  }

  @Provides
  AcceptanceFunction acceptFun(OptimizerSpec spec,
      Map<SerialAcceptanceFunction, Provider<AcceptanceFunction>> map) {
    return map.get(spec.getSubAcceptFun()).get();
  }

  public enum PredefinedTemperatureFunctions implements TemperatureFunction {
    LINEAR_FUNCTION {
      @Override
      public double temperature(int currentStep, int nSteps) {
        return ((double) (nSteps - 1 - currentStep)) / nSteps;
      }
    },
    QUADRATIC_FUNCTION {
      @Override
      public double temperature(int currentStep, int nSteps) {
        int numerator = nSteps - 1 - currentStep;
        numerator *= numerator;
        return ((double) numerator) / (nSteps * nSteps);
      }
    },
    FLAT {
      @Override
      public double temperature(int currentStep, int nSteps) {
        return 0.5;
      }
    };
  }
}
