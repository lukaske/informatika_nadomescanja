import boto3
import time
import requests
from bs4 import BeautifulSoup

def getLinks():
    nadomescanja = []

    login_url = "https://ucilnica.gimvic.org/login/index.php"
    okroznice_url = "https://ucilnica.gimvic.org/course/view.php?id=118"

    s = requests.session()

    payload = "username=name&password=pass&anchor"
    headers = {
        'Host': "ucilnica.gimvic.org",
        'Connection': "keep-alive",
        'Content-Length': "44",
        'Cache-Control': "no-cache",
        'Origin': "https://ucilnica.gimvic.org",
        'Upgrade-Insecure-Requests': "1",
        'Content-Type': "application/x-www-form-urlencoded",
        'User-Agent': "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36",
        'Accept': "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
        'Referer': "https://ucilnica.gimvic.org/login/index.php",
        'Accept-Encoding': "gzip, deflate, br",
        'Accept-Language': "en-GB,en-US;q=0.9,en;q=0.8,sl;q=0.7",
        'Postman-Token': "b497d4be-82f1-4600-9358-4e3823caff0b"
    }
    x = s.post(login_url, data=payload, headers=headers)
    url_to_parse = s.get(okroznice_url)
    soup = BeautifulSoup(url_to_parse.text, "lxml")
    a = soup.select("div.activityinstance")
    print(a)
    a = soup.select("a[onclick^=window.open]")

    for element in a:
        print(element)
        url = element["onclick"].split("'")[1]
        response = s.get(url)
        url = response.url
        url = url[:(len(url) - 1)]
        url = url + "0"
        name = element.text
        nadomescanja.append({name: url})
    print("Downloaded data", nadomescanja)
    return nadomescanja
def linkpayloadbuilder(acquired_links):
    payload_inside = {}
    for item in acquired_links:
        for key, value in item.items():
            payload_inside[key] = {'S':value}
    payload = {}
    payload['M'] = payload_inside
    print(payload)
    return payload



def lambda_handler(event, context):


    timestamp = str(int(time.time()))
    db_json = {}
    db_json['timestamps'] = {"N": timestamp}
    db_json['links'] = linkpayloadbuilder(getLinks())
    dynamodb = boto3.client('dynamodb')
    dynamodb.put_item(TableName='nadomescanja_links', Item=db_json)
    dynamodb.update_item(TableName='nadomescanja_links', Key={"timestamps": {"N": "0"}},AttributeUpdates={"current": {"Value": {'N': timestamp}}})

    return {
        'statusCode': 200,
        'body': 1
    }

