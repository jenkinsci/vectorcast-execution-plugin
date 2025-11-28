from lxml import etree

import glob, sys

validateDict = {'xml_data/test_results_*.xml' : 'JUnit.xsd',
    'xml_data/**/test_results_*.xml' : 'JUnit.xsd',
    'xml_data/cobertura/coverage_results_*.xml' : 'coverage-extended-04.dtd'
}
def getValidator(schema):
    validator = None
    
    if schema.endswith('.xsd'):
        validator = etree.XMLSchema(etree.parse(schema))
    elif schema.endswith('.dtd'):
        with open(schema) as f:
            validator = etree.DTD(f)
    return validator

error_count = 0
    
for key in validateDict.keys():
    
    for file in glob.glob(key):
        xml = etree.parse(file)
        validator = getValidator(validateDict[key])

        if not validator.validate(xml):
            print(file, "is invalid per schema", validateDict[key])
            print(validator.error_log)
            error_count += 1

if error_count == 0:
    print("All xml_data files validate")
    
sys.exit(error_count)