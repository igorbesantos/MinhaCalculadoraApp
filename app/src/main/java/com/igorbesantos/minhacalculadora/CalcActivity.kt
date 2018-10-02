package com.igorbesantos.minhacalculadora

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.igorbesantos.minhacalculadora.utils.contemPonto
import java.math.BigDecimal

/***
 * Activity principal, a calculadora em si
 * @author Igor Santos
 */
class CalcActivity : AppCompatActivity() {

    /***
     * TextView que exibe input do usuário e o resultado das operações ("Display" da calculadora)
     */
    var tv_display: TextView? = null

    /***
     * Memória de operação
     * Armazena cinco estados, as quatro operações e um estado vazio
     */
    var operacao = ""

    /***
     * Memória de valor
     * Armazena um segundo valor (além do exibido no display) ou um estado vazio/nulo
     */
    var n1: BigDecimal? = null

    /***
     * Memória secundária de valor
     * Guarda último valor DIGITADO pelo usuário (não calculado), quando é relevante (ou um estado vazio/nulo)
     */
    var ultimoInseridoManualmente: BigDecimal? = null

    /***
     * Flag para indicar se o valor exibido no display está sendo (e pode ser) editado ou já foi inserido por completo
     */
    var isValorDisplayEditavel = true

    /***
     * Flag para indicar se os próximos dígitos devem ser colocados em casas decimais
     */
    var isEntradaDecimal = false

    /***
     * Flag para indicar se a operação deve ser realizada
     */
    var podeCalcular = false

    /***
     * Flag para indicar se o botão de igual está sendo pressionado consecutivamente
     */
    var isSequenciaIgualAtivada = false

    val MAX_DIGITOS = 15

    /* Constantes da operações */
    val SOMA = "+"
    val SUBTRACAO = "-"
    val DIVISAO = "/"
    val MULTIPLICACAO = "*"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calc)
        tv_display = findViewById(R.id.tv_display)
        tv_display?.text = "0"
    }

    /***
     * Trata o clique de todos os botões da calculadora
     */
    fun gerenciarClique(view: View){
        when(view.id){
            R.id.btn_limpar -> limpar()
            R.id.btn_somar ->{
                verificaParadaIguaisConsecutivos()
                calcularNumeros(SOMA)
            }
            R.id.btn_multiplicar -> {
                verificaParadaIguaisConsecutivos()
                calcularNumeros(MULTIPLICACAO)
            }
            R.id.btn_dividir -> {
                verificaParadaIguaisConsecutivos()
                calcularNumeros(DIVISAO)
            }
            R.id.btn_subtrair -> {
                verificaParadaIguaisConsecutivos()
                calcularNumeros(SUBTRACAO)
            }
            R.id.btn_igual -> executarIgual()
            else -> {
                interpretarDigitacao((view as Button).text.toString())
            }
        }
    }

    /***
     * Volta a calculadora para seu estado inicial
     */
    fun limpar() {
        findViewById<TextView>(R.id.tv_display)?.text = "0"
        n1 = null
        ultimoInseridoManualmente = null
        operacao = ""
        isEntradaDecimal = false
    }

    /***
     * Atribui o resultado de executarOperacao() a n1,
     * exibe o resultado , atualiza ou limpa operação guardada (de acordo com parâmetro)
     * e marca o display atual como apenas para exibição
     */
    fun calcularNumeros(operacao: String){
        //Cancela a entrada do dígito decimal se qualquer operação for pressionada
        isEntradaDecimal = false

        val valorInserido = BigDecimal(tv_display?.text?.toString())

        /* Atualiza o último valor digitado
         * OBS.: Se n1 for nulo aqui, significa que o usuário pressionou o botão "igual" pelo
         *       menos pela segunda vez consecutiva, e nesse caso a operação deve ser executada
         *       com o último valor inserido manualmente, e não com o novo valor (acumulado)
         */
        if(n1 != null){
            ultimoInseridoManualmente = valorInserido
        }else if(ultimoInseridoManualmente != null){
            n1 = ultimoInseridoManualmente
        }

        var n2: BigDecimal = valorInserido

        //Acertar ordem dos fatores
        //Ex.: Para ser possível continuar dividindo o total por dois, não para continuar dividindo dois pelo total
        if(isSequenciaIgualAtivada && n1 != null){
            n2 = n1!!
            n1 = valorInserido
        }

        n1 = executarOperacao(n1, n2, this.operacao)

        //A operação não deve ser executada caso botões de operação sejam pressionados consecutivamente (exceto o igual)
        podeCalcular = false

        if(isTamanhoTextoValido(n1?.toPlainString()?.length!!, "O número máximo de dígitos ($MAX_DIGITOS) foi atingido pela resposta.")) {
            this.operacao = operacao
            tv_display?.text = formataParaExibicao(n1?.toPlainString())
            isValorDisplayEditavel = false
        }else{
            limpar()
        }
    }

    /***
     * Caso n1 seja nulo ou operação esteja vazia, retorna n2,
     * caso contrário, retorna o resultado da operação
     */
    fun executarOperacao(n: BigDecimal?, n2: BigDecimal, operacaoInserida: String): BigDecimal{
        var n1 = if(n == null) BigDecimal(0) else n
        if(podeCalcular) {
            return when(operacaoInserida){
                SOMA -> n1.add(n2)
                SUBTRACAO -> n1.subtract(n2)
                MULTIPLICACAO -> n1.multiply(n2)
                DIVISAO -> {
                    if(n2.toPlainString().equals("0")) n2 else n1.divide(n2)
                }
                else -> n2
            }
        }
        return n2
    }

    /***
     * Define o comportamento do botão igual
     * Caso ele seja pressionado várias vezes a calculadora repeta a última operação com
     * o valor acumulado atual e o último valor inserido manualmente pelo usuário
     */
    fun executarIgual(){
        if(this.operacao.isNotEmpty()) {
            val op = this.operacao
            calcularNumeros(op)
            n1 = null
            //Permite refazer a última operação indefinidas vezes, pressionando o igual consecutivamente
            podeCalcular = true
            isSequenciaIgualAtivada = true
        }
    }

    /***
     * Valida e concatena os dígitos pressionados pelo usuário,
     * para formar corretamente o "texto" correspondente ao valor que está sendo inserido
     */
    fun interpretarDigitacao(entrada: String){

        isSequenciaIgualAtivada = false

        //Permite efetuar o cálculo novamente ao inserir novo valor
        podeCalcular = true

        if(isValorDisplayEditavel) {
            //Valida tamanho previsto da entrada do usuário
            var tamanhoTexto = 0
            tamanhoTexto += tv_display?.text?.length!!
            tamanhoTexto += entrada.length
            if (isEntradaDecimal) tamanhoTexto++
            if (!isTamanhoTextoValido(tamanhoTexto)) return
        }

        //Se qualquer dígito é pressionado o último valor inserido manualmente deve ser desconsiderado
        ultimoInseridoManualmente = null

        //Se valor não é editável e um dígito é pressionado, o valor exibido é descartado
        //OBS.: O valor exibido e não editável ainda é usado caso um botão de operação for pressionado
        if(!isValorDisplayEditavel){
            tv_display?.text = "0"
            isValorDisplayEditavel = true
        }

        val textoDisplay = tv_display?.text?.toString()

        //Se é o primeiro dígito relevante a ser pressionado, substitui o zero exibido
        if(textoDisplay == "0" && !isEntradaDecimal && entrada != "." && entrada[0] != '0'){
            tv_display?.text = entrada
            return
        }

        //Valida a inserção do ponto
        if(entrada == "."){
            if(isValorDisplayEditavel && textoDisplay == "0"){
                isEntradaDecimal = true
                return
            }
            if(textoDisplay!!.contemPonto().not()) isEntradaDecimal = true
            return
        }

        //Concatena o dígito referente ao botão pressionado
        if(isEntradaDecimal){
            tv_display?.text = formataParaExibicao("$textoDisplay.$entrada")
            isEntradaDecimal = false
        }else {
            tv_display?.text = formataParaExibicao("$textoDisplay$entrada")
        }
    }

    /***
     * Ajusta o estado da calculadora para realizar uma operação diferente, caso esteja sendo feita logo após o igual ser pressionado múltiplas vezes consecutivas
     */
    fun verificaParadaIguaisConsecutivos(){
        if(isSequenciaIgualAtivada){
            podeCalcular = false
            isSequenciaIgualAtivada = false
        }
    }

    /***
     * Verifica se o tamanho do texto ultrapassa o limite e exibe aviso em um Toast
     */
    private fun isTamanhoTextoValido(tamanhotexto: Int, mensagem: String = "O número máximo de dígitos ($MAX_DIGITOS) foi atingido."): Boolean{
        if(tamanhotexto > MAX_DIGITOS){
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    /***
     * Retira o ponto, os zeros à direita (do ponto) e os zeros à esquerda, quando não são necessários
     */
    private fun formataParaExibicao(valor: String?):String{
        if(valor != null) {
            var formatado: String = valor
            if (valor.contemPonto()) {
                //Retira zeros irrelevantes à direita do ponto
                var pos = valor.length - 1
                while (formatado.get(pos) == '0') {
                    formatado = formatado.substring(0, pos)
                    pos--
                }
                //Retira o ponto caso não haja valores decimais
                pos = formatado.length - 1
                if (formatado.get(pos) == '.') {
                    formatado = formatado.substring(0, pos)
                }
            }
            while(formatado.length > 1 && formatado[0] == '0' && formatado[1] != '.'){
                formatado = formatado.substring(1, formatado.length)
            }
            return formatado
        }else{
            return "0"
        }
    }

}
