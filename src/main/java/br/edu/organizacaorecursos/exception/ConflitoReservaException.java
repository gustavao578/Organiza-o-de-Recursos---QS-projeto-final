package br.edu.organizacaorecursos.exception;

public class ConflitoReservaException extends RegraNegocioException {

    private final String detalhes;

    public ConflitoReservaException(String mensagem, String detalhes) {
        super(mensagem);
        this.detalhes = detalhes;
    }

    public String getDetalhes() {
        return detalhes;
    }
}
