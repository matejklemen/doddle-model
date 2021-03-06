package io.picnicml.doddlemodel.impute

import breeze.linalg.{DenseMatrix, DenseVector}
import io.picnicml.doddlemodel.TestingUtils
import io.picnicml.doddlemodel.data.Feature.{CategoricalFeature, FeatureIndex, NumericalFeature}
import io.picnicml.doddlemodel.impute.MostFrequentValueImputer.ev
import org.scalatest.{FlatSpec, Matchers, OptionValues}

class MostFrequentValueImputerTest extends FlatSpec with Matchers with TestingUtils with OptionValues {

  "Most frequent value imputer" should "impute the categorical features" in {
    val xMissing = DenseMatrix(
      List(Double.NaN, 1.0, 2.0),
      List(3.0, Double.NaN, 5.0),
      List(6.0, 7.0, 8.0),
      List(6.0, 7.0, 2.0)
    )

    val xImputedExpected = DenseMatrix(
      List(6.0, 1.0, 2.0),
      List(3.0, Double.NaN, 5.0),
      List(6.0, 7.0, 8.0),
      List(6.0, 7.0, 2.0)
    )

    val featureIndex = FeatureIndex.apply(List(CategoricalFeature, NumericalFeature, CategoricalFeature))
    val imputer = MostFrequentValueImputer(featureIndex)
    val fittedImputer = ev.fit(imputer, xMissing)

    breezeEqual(fittedImputer.mostFrequent.value, DenseVector(6.0, 2.0)) shouldBe true
    breezeEqual(ev.transform(fittedImputer, xMissing), xImputedExpected) shouldBe true
  }

  it should "impute a subset of categorical features" in {
    val xMissing = DenseMatrix(
      List(Double.NaN, 1.0, 2.0),
      List(3.0, Double.NaN, 5.0),
      List(6.0, 7.0, 8.0),
      List(6.0, 7.0, 2.0)
    )

    val xImputedExpected = DenseMatrix(
      List(Double.NaN, 1.0, 2.0),
      List(3.0, 7.0, 5.0),
      List(6.0, 7.0, 8.0),
      List(6.0, 7.0, 2.0)
    )

    val featureIndex = FeatureIndex.categorical(List(1, 2))
    val imputer = MostFrequentValueImputer(featureIndex)
    val fittedImputer = ev.fit(imputer, xMissing)

    breezeEqual(fittedImputer.mostFrequent.value, DenseVector(7.0, 2.0)) shouldBe true
    breezeEqual(ev.transform(fittedImputer, xMissing), xImputedExpected) shouldBe true
  }
}
