package com.igorbesantos.minhacalculadora.utils

/***
 * Retorna true se a String contiver o caracter '.'
 */
fun String.contemPonto():Boolean{
    var contemPonto = false
    this.forEach {if(it == '.') contemPonto = true}
    return contemPonto
}