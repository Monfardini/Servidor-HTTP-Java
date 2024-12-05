import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorHTTP {

    // Simula um banco de dados em memória
    private static Map<Integer, Aluno> alunos = new HashMap<>();
    
    // Controle de IDs para garantir unicidade
    private static AtomicInteger nextId = new AtomicInteger(1);

    public static void main(String[] args) throws IOException {
        // Cria o servidor HTTP
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Define as rotas
        server.createContext("/aluno", new AlunoHandler());
        server.createContext("/aluno/", new AlunoByIdHandler());

        // Inicia o servidor
        server.start();
        System.out.println("Servidor rodando na porta 8080...");
    }

    // Handler para rotas gerais relacionadas a /aluno
    static class AlunoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><body><h1>Metodo não suportado na raiz de /aluno</h1></body></html>";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Handler para operações em /aluno/:id
    static class AlunoByIdHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length != 3) {
                // URL inválida
                sendResponse(exchange, "<html><body><h1>Erro 404</h1><p>URL inválida!</p></body></html>", 404);
                return;
            }

            int id;
            try {
                id = Integer.parseInt(pathParts[2]);
            } catch (NumberFormatException e) {
                sendResponse(exchange, "<html><body><h1>Erro 404</h1><p>ID inválido!</p></body></html>", 404);
                return;
            }

            switch (exchange.getRequestMethod()) {
                case "GET":
                    handleGetAluno(id, exchange);
                    break;

                case "DELETE":
                    handleDeleteAluno(id, exchange);
                    break;

                case "POST":
                    handlePostAluno(exchange);
                    break;

                default:
                    sendResponse(exchange, "<html><body><h1>Erro 405</h1><p>Método não permitido!</p></body></html>", 405);
            }
        }

        // GET /aluno/:id
        private void handleGetAluno(int id, HttpExchange exchange) throws IOException {
            synchronized (alunos) {
                Aluno aluno = alunos.get(id);
                if (aluno != null) {
                    String response = "<html><body><h1>Aluno " + id + "</h1><p>Nome: " + aluno.getNome() + "</p></body></html>";
                    sendResponse(exchange, response, 200);
                } else {
                    String response = "<html><body><h1>Erro 404</h1><p>Aluno não encontrado</p></body></html>";
                    sendResponse(exchange, response, 404);
                }
            }
        }

        // DELETE /aluno/:id
        private void handleDeleteAluno(int id, HttpExchange exchange) throws IOException {
            synchronized (alunos) {
                Aluno aluno = alunos.remove(id);
                if (aluno != null) {
                    String response = "<html><body><h1>Aluno " + id + " excluído com sucesso!</h1></body></html>";
                    sendResponse(exchange, response, 200);
                } else {
                    String response = "<html><body><h1>Erro 404</h1><p>Aluno não encontrado</p></body></html>";
                    sendResponse(exchange, response, 404);
                }
            }
        }

        // POST /aluno
        private void handlePostAluno(HttpExchange exchange) throws IOException {
            synchronized (alunos) {
                // Criação de aluno com ID único
                int id = nextId.getAndIncrement();
                String nome = "Aluno " + id;
                Aluno aluno = new Aluno(id, nome);
                alunos.put(id, aluno);
                String response = "<html><body><h1>Aluno criado com sucesso!</h1><p>ID: " + id + " Nome: " + nome + "</p></body></html>";
                sendResponse(exchange, response, 201);
            }
        }

        // Método auxiliar para enviar a resposta
        private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Classe para representar um Aluno
    static class Aluno {
        private int id;
        private String nome;

        public Aluno(int id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        public int getId() {
            return id;
        }

        public String getNome() {
            return nome;
        }
    }
}
