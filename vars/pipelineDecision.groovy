def decidePipeline(Map configMap){
    switch(configMap.application) {
        case 'nodejsVM':
            nodejsVM(configMap);
            break;
    }
}