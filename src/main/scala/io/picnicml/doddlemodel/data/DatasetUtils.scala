package io.picnicml.doddlemodel.data

import breeze.stats.hist

import scala.util.Random

object DatasetUtils {

  /** Shuffles rows of the dataset.
    * @param x features to be shuffled
    * @param y labels corresponding to features
    *
    * @example Shuffle a dataset randomly.
    *   {{{
    *     import scala.util.Random
    *
    *     // we are assuming data was previously loaded
    *     val (dataX, dataY) = ...
    *
    *     val (shuffledX, shuffledY) = shuffleDataset(dataX, dataY)
    *
    *     // seeded shuffle - seed passed to shuffler implicitly
    *     implicit val rand: Random = new Random(42)
    *     val (shuffledX, shuffledY) = shuffleDataset(dataX, dataY)
    *   }}}
    * */
  def shuffleDataset(x: Features, y: Target)(implicit rand: Random = new Random()): Dataset = {
    val shuffleIndices = rand.shuffle((0 until y.length).toIndexedSeq)
    (x(shuffleIndices, ::).toDenseMatrix, y(shuffleIndices).toDenseVector)
  }

  /** Splits the dataset into two subsets for training and testing.
    * @param x features to be split
    * @param y labels corresponding to features
    * @param proportionTrain proportion of dataset to be put into training set - between 0.0 and 1.0
    *
    * @example Split dataset into training and test set.
    *   {{{
    *     // we are assuming data was previously loaded
    *     val (dataX, dataY) = ...
    *
    *     // by default, the split is 50%:50%
    *     val trTeSplit = splitDataset(dataX, dataY)
    *
    *     // put 80% of data into training set and 20% into test set
    *     val trTeSplit = splitDataset(dataX, dataY, 0.8)
    *     val (trainX, trainY, testX, testY) = (trTeSplit.xTr, trTeSplit.yTr, trTeSplit.xTe, trTeSplit.yTe)
    *   }}}
    * */
  def splitDataset(x: Features, y: Target, proportionTrain: Float = 0.5f): TrainTestSplit = {
    val numTrain = numberOfTrainExamplesBasedOnProportion(x.rows, proportionTrain)
    val trIndices = 0 until numTrain
    val teIndices = numTrain until x.rows
    TrainTestSplit(x(trIndices, ::), y(trIndices), x(teIndices, ::), y(teIndices))
  }

  /** Splits the dataset into two subsets for training and testing and makes sure groups in each are non-overlapping. */
  def splitDatasetWithGroups(x: Features,
                             y: Target,
                             groups: IntVector,
                             proportionTrain: Float = 0.5f): GroupTrainTestSplit = {
    val numTrain = numberOfTrainExamplesBasedOnProportion(x.rows, proportionTrain)
    val numSamplesPerGroup = hist(groups, numberOfUniqueGroups(groups)).hist.toArray
    val (sortedNumSamplesPerGroup, toOriginalGroupIndex) = numSamplesPerGroup.zipWithIndex.sorted.unzip

    val numGroupsInTrain = sortedNumSamplesPerGroup
      .foldLeft(List(0)) { case (acc, currGroupSize) => (acc(0) + currGroupSize) :: acc }.reverse.drop(1)
      .takeWhile(cumulativeNumSamples => cumulativeNumSamples <= numTrain)
      .length

    val groupsInTrain = (0 until numGroupsInTrain).map(group => toOriginalGroupIndex(group))

    val (trIndices, teIndices) = (0 until groups.length).foldLeft((IndexedSeq[Int](), IndexedSeq[Int]())) {
      case ((currTrIndices, currTeIndices), groupIndex) =>
        if (groupsInTrain.contains(groups(groupIndex)))
          (currTrIndices :+ groupIndex, currTeIndices)
        else
          (currTrIndices, currTeIndices :+ groupIndex)
    }

    GroupTrainTestSplit(
      x(trIndices, ::).toDenseMatrix,
      y(trIndices).toDenseVector,
      groups(trIndices).toDenseVector,
      x(teIndices, ::).toDenseMatrix,
      y(teIndices).toDenseVector,
      groups(teIndices).toDenseVector
    )
  }

  private def numberOfTrainExamplesBasedOnProportion(numTotal: Int, proportionTrain: Float): Int = {
    require(proportionTrain > 0.0 && proportionTrain < 1.0, "proportionTrain must be between 0 and 1")
    val numTrain = (proportionTrain * numTotal.toFloat).toInt
    require(numTrain > 0 && numTrain < numTotal, "the value of proportionTrain is either too high or too low")
    numTrain
  }
}
