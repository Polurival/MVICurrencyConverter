package com.github.polurival.mvicurrencyconverter.cbrf

import com.github.polurival.mvicurrencyconverter.dto.CbrfResponse
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import kotlinx.coroutines.Deferred
import org.w3c.dom.Document
import java.io.InputStream
import java.net.URL
import java.net.UnknownServiceException
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

/**
 * @author Польщиков Юрий on 2019-07-06
 */
class ManualCbrfApiFacade : CbrfApi {

    override fun loadCurrenciesInfo(): CbrfResponse {
        val url = URL(API_URL)
        val connection = url.openConnection()
        var inputStream: InputStream? = null
        try {
            inputStream = connection.getInputStream()
        } catch (e: Throwable) {
            var x = 0
        }
        val xmlDocument = parseDataToXmlDocument(inputStream)

        return convertDocumentToListOfCurrencies(xmlDocument)
    }

    private fun parseDataToXmlDocument(inputStream: InputStream?): Document {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = documentBuilder.parse(inputStream)

        inputStream?.close()
        return document
    }

    private fun convertDocumentToListOfCurrencies(document: Document): CbrfResponse {
        val root = document.getElementsByTagName(ROOT_TAG)
        val date = root.item(0).attributes.getNamedItem(DATE_ATTR).textContent
        val currencies = TreeMap<String, CurrencyInfo>()

        val valutes = document.getElementsByTagName(VALUTE_TAG)
        for (i in 0 until valutes.length) {
            var charCode: String? = null
            var nominal: Int? = null
            var name: String? = null
            var value: String? = null

            val valute = valutes.item(i).childNodes
            for (j in 0 until valute.length) {
                val nodeName = valute.item(j).nodeName
                val textContent = valute.item(j).textContent

                when (nodeName) {
                    CHAR_CODE_TAG -> charCode = textContent
                    NOMINAL_TAG -> nominal = textContent.toInt()
                    NAME_TAG -> name = textContent
                    VALUE_TAG -> value = textContent.replace(',', '.')
                }

                if (charCode != null && nominal != null && name != null && value != null) {
                    currencies.put(charCode, CurrencyInfo(charCode, nominal, name, value))
                    break
                }
            }
        }
        return CbrfResponse(date, currencies)
    }

    companion object {
        val API_URL = "http://www.cbr.ru/scripts/XML_daily.asp"
        val ROOT_TAG = "ValCurs"
        val DATE_ATTR = "Date"
        val VALUTE_TAG = "Valute"
        val CHAR_CODE_TAG = "CharCode"
        val NOMINAL_TAG = "Nominal"
        val NAME_TAG = "Name"
        val VALUE_TAG = "Value"

        val service = ManualCbrfApiFacade()
    }
}