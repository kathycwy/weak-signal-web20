# weak-signal-web20
MA Wing Yin Chan - Organizing Computer Science Topics from Web 2.0 Tools

## Steps to regenerate the results

1. Data Collection
    1. Update your preferred website details in WebsiteDetails.java
    2. Run CrawlService.java to create raw.csv

2. Text Cleaning
    1. Run NlpService.java to create raw-clean.csv

3. Topic Modelling
    1. Run TopicModellingService.class
    2. MALLET runs in command line, if the function is not working, please run the below two commands in terminal
  ```
  Mallet-202108/bin/mallet import-dir --input Mallet-202108/data --output Mallet-202108/output/input.mallet --keep-sequence
  ```
  ```
  Mallet-202108/bin/mallet train-topics  --input Mallet-202108/output/input.mallet --num-topics 100 --optimize-interval 10 --output-state Mallet-202108/output/topic-state.gz --output-topic-keys Mallet-202108/output/keys.txt --output-doc-topics Mallet-202108/output/composition.txt
  ``` 

4. Database Connection
    1. Create your AuraDB account at [https://neo4j.com/cloud/platform/aura-graph-database/](https://neo4j.com/cloud/platform/aura-graph-database/)
    2. Then create a new database instance, copy and save the uri and credential details
    3. Update main() in Neo4jConnector.java with your database uri and credentials
    4. Run prepareDbData() in Neo4jConnector.java to prepare a CSV file that contain the data for writing to database
    5. Upload that CSV file to a online space (i.e. GitHub), the URL of the CSV will be needed when importing it to AuraDB as it is a cloud service
    6. Update the URLs of the CSV files in all createWebsiteAndDocument(), createTopicAndWord() and createTopicLabel() functions in Neo4jConnector.java with your URLs
    7. Run main() in Neo4jConnector.java to write and read data from database

5. Weak Signal Score Calculation
    1. Run WeakSignalCalculator.java to create WeakSignalValues.csv
    2. Upload this CSV file to a online space (i.e. GitHub)
    3. Update the URL of CSV in createScore() in Neo4jConnector.java with your URL
    4. Run createScore() in Neo4jConnector.java to write scores to database

