package iris;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;

public class App1stTrain {


    public static final String DIR_ROOT = System.getProperty("user.dir");
    public static final String DIR_APP = DIR_ROOT+"/yggdrasil-app/iris";
    public static final String DIR_DATA = DIR_ROOT+"/yggdrasil-data/iris";
    public static final String FILE_FULL = DIR_DATA+"/iris-full.txt";
    public static final String FILE_TRAIN = DIR_DATA+"/iris-train.bin";
    public static final String FILE_TEST = DIR_DATA+"/iris-test.bin";
    public static final String FILE_NORM = DIR_DATA+"/iris-norm.bin";
    public static final String FILE_NEURAL = DIR_DATA+"/iris-neural.zip";

    public static void main(String[] args) throws IOException, InterruptedException {

        // 1. Parâmetros da base de dados e da rede
        int numLinesToSkip = 0; // Mude para 1 se o iris.txt tiver cabeçalho
        char delimiter = ',';
        int labelIndex = 4;     // Índice da coluna de classe (0 a 3 são entradas, 4 é a saída)
        int numClasses = 3;     // 3 espécies de Iris
        int batchSize = 150;    // Total de amostras na base Iris

        // 2. Leitura do arquivo iris.txt
        RecordReader recordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        recordReader.initialize(new FileSplit(new File(App1stTrain.FILE_FULL)));

        // Converte os dados lidos em um DataSet com One-Hot Encoding automático para as 3 classes
        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, batchSize, labelIndex, numClasses);
        DataSet allData = iterator.next();
        allData.shuffle(42); // Embaralha os dados usando uma semente fixa

        // 3. Divisão em Treino (80%) e Teste (20%)
        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.80);

        DataSet trainingData = testAndTrain.getTrain();
        trainingData.save(new File(App1stTrain.FILE_TRAIN));

        DataSet testData = testAndTrain.getTest();
        testData.save(new File(App1stTrain.FILE_TEST));

        // 4. Normalização dos dados (Média 0 e Desvio Padrão 1)
        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(trainingData);           // Calcula estatísticas apenas no treino
        normalizer.transform(trainingData);     // Aplica no treino
        normalizer.transform(testData);         // Aplica no teste        
        File normalizerFile = new File(App1stTrain.FILE_NORM);
        NormalizerSerializer.getDefault().write(normalizer, normalizerFile);

        // 5. Configuração da Arquitetura MLP (4 -> 8 -> 3)
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(42)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(0.01)) // Otimizador Adam com taxa de aprendizado 0.01
                .list()
                // Camada Oculta (Recebe 4 entradas, possui 8 neurônios e ativação ReLU)
                .layer(new DenseLayer.Builder()
                        .nIn(4)
                        .nOut(8)
                        .activation(Activation.RELU)
                        .build())
                // Camada de Saída (Recebe 8 entradas, possui 3 saídas e ativação Softmax)
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nIn(8)
                        .nOut(numClasses)
                        .activation(Activation.SOFTMAX)
                        .build())
                .build();

        // 6. Inicialização e Treinamento do Modelo
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        // Exibe o erro (loss) a cada 20 iterações
        model.setListeners(new ScoreIterationListener(20));

        int epochs = 200;
        for (int i = 0; i < epochs; i++) {
            model.fit(trainingData);
        }

        model.save(new File(App1stTrain.FILE_NEURAL));

        // 7. Avaliação nos dados de teste
        Evaluation eval = new Evaluation(numClasses);
        INDArray output = model.output(testData.getFeatures());
        eval.eval(testData.getLabels(), output);

        System.out.println("=== Resultados da Avaliação ===");
        System.out.println(eval.stats());
    }
}