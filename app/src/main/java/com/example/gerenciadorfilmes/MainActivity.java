
package com.example.gerenciadorfilmes;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etNome, etAno, etNota;
    private Spinner spTipo, spGenero, spFiltro;
    private CheckBox cbAssistido;
    private Button btnSalvar;
    private ListView lvTitulos;

    private FirebaseFirestore db;
    private final List<DocumentSnapshot> documentos = new ArrayList<>();
    private final List<String> textos = new ArrayList<>();
    private ArrayAdapter<String> adaptadorLista;
    private String idEmEdicao = null;

    private final String[] tipos = {
            "Filme", "Série", "Documentário", "Animação"
    };

    private final String[] generos = {
            "Ação", "Comédia", "Drama", "Terror",
            "Ficção Científica"
    };

    private final String[] filtros = {
            "Todos", "Filme", "Série",
            "Documentário", "Animação"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNome = findViewById(R.id.etNome);
        etAno = findViewById(R.id.etAno);
        etNota = findViewById(R.id.etNota);
        spTipo = findViewById(R.id.spTipo);
        spGenero = findViewById(R.id.spGenero);
        spFiltro = findViewById(R.id.spFiltro);
        cbAssistido = findViewById(R.id.cbAssistido);
        btnSalvar = findViewById(R.id.btnSalvar);
        lvTitulos = findViewById(R.id.lvTitulos);

        db = FirebaseFirestore.getInstance();

        configurarSpinner(spTipo, tipos);
        configurarSpinner(spGenero, generos);
        configurarSpinner(spFiltro, filtros);

        adaptadorLista = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                textos
        );
        lvTitulos.setAdapter(adaptadorLista);

        btnSalvar.setOnClickListener(v -> salvarTitulo());

        spFiltro.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view, int position, long id) {
                        carregarTitulos();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );

        lvTitulos.setOnItemClickListener((parent, view, position, id) -> {
            if (position < documentos.size()) {
                carregarParaEdicao(documentos.get(position));
            }
        });

        lvTitulos.setOnItemLongClickListener(
                (parent, view, position, id) -> {
                    if (position < documentos.size()) {
                        confirmarExclusao(documentos.get(position));
                    }
                    return true;
                }
        );

        carregarTitulos();
    }

    private void configurarSpinner(Spinner spinner, String[] opcoes) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                opcoes
        );
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinner.setAdapter(adapter);
    }

    private void salvarTitulo() {
        String nome = etNome.getText().toString().trim();
        String anoTexto = etAno.getText().toString().trim();
        String notaTexto = etNota.getText().toString().trim();

        if (nome.isEmpty() || anoTexto.isEmpty() || notaTexto.isEmpty()) {
            Toast.makeText(this,
                    "Preencha todos os campos obrigatórios.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int ano;
        double nota;

        try {
            ano = Integer.parseInt(anoTexto);
            nota = Double.parseDouble(notaTexto.replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.makeText(this,
                    "Confira o ano e a nota informados.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (ano <= 0 || nota < 0 || nota > 10) {
            Toast.makeText(this,
                    "Informe um ano válido e uma nota de 0 a 10.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> titulo = new HashMap<>();
        titulo.put("nome", nome);
        titulo.put("tipo", spTipo.getSelectedItem().toString());
        titulo.put("genero", spGenero.getSelectedItem().toString());
        titulo.put("ano", ano);
        titulo.put("nota", nota);
        titulo.put("assistido", cbAssistido.isChecked());

        if (idEmEdicao == null) {
            db.collection("titulos")
                    .add(titulo)
                    .addOnSuccessListener(documento -> {
                        Toast.makeText(this, "Título salvo!",
                                Toast.LENGTH_SHORT).show();
                        limparFormulario();
                        carregarTitulos();
                    })
                    .addOnFailureListener(e -> mostrarErro(e));
        } else {
            db.collection("titulos")
                    .document(idEmEdicao)
                    .update(titulo)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Título atualizado!",
                                Toast.LENGTH_SHORT).show();
                        limparFormulario();
                        carregarTitulos();
                    })
                    .addOnFailureListener(e -> mostrarErro(e));
        }
    }

    private void carregarTitulos() {
        String filtro = spFiltro.getSelectedItem() == null
                ? "Todos" : spFiltro.getSelectedItem().toString();

        com.google.firebase.firestore.Query consulta =
                db.collection("titulos");

        if (!filtro.equals("Todos")) {
            consulta = consulta.whereEqualTo("tipo", filtro);
        }

        consulta.get()
                .addOnSuccessListener(resultado -> {
                    documentos.clear();
                    textos.clear();

                    for (DocumentSnapshot doc : resultado.getDocuments()) {
                        documentos.add(doc);

                        String nome = doc.getString("nome");
                        String tipo = doc.getString("tipo");
                        String genero = doc.getString("genero");
                        Long ano = doc.getLong("ano");
                        Double nota = doc.getDouble("nota");
                        Boolean assistido = doc.getBoolean("assistido");

                        String linha = nome + " | " + tipo
                                + "\nGênero: " + genero
                                + " | Ano: " + ano
                                + " | Nota: " + nota
                                + "\nAssistido: "
                                + (Boolean.TRUE.equals(assistido)
                                ? "Sim" : "Não");

                        textos.add(linha);
                    }

                    adaptadorLista.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> mostrarErro(e));
    }

    private void carregarParaEdicao(DocumentSnapshot doc) {
        etNome.setText(doc.getString("nome"));

        selecionar(spTipo, doc.getString("tipo"));
        selecionar(spGenero, doc.getString("genero"));

        Long ano = doc.getLong("ano");
        Double nota = doc.getDouble("nota");
        Boolean assistido = doc.getBoolean("assistido");

        etAno.setText(ano == null ? "" : String.valueOf(ano));
        etNota.setText(nota == null ? "" : String.valueOf(nota));
        cbAssistido.setChecked(Boolean.TRUE.equals(assistido));

        idEmEdicao = doc.getId();
        btnSalvar.setText("Atualizar");

        Toast.makeText(this,
                "Edite os campos e toque em Atualizar.",
                Toast.LENGTH_SHORT).show();
    }

    private void selecionar(Spinner spinner, String valor) {
        if (valor == null) return;

        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(valor)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void confirmarExclusao(DocumentSnapshot doc) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir título")
                .setMessage("Deseja realmente excluir \""
                        + doc.getString("nome") + "\"?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    db.collection("titulos")
                            .document(doc.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Título excluído!",
                                        Toast.LENGTH_SHORT).show();

                                if (doc.getId().equals(idEmEdicao)) {
                                    limparFormulario();
                                }

                                carregarTitulos();
                            })
                            .addOnFailureListener(e -> mostrarErro(e));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void limparFormulario() {
        etNome.setText("");
        etAno.setText("");
        etNota.setText("");
        spTipo.setSelection(0);
        spGenero.setSelection(0);
        cbAssistido.setChecked(false);
        idEmEdicao = null;
        btnSalvar.setText("Salvar");
    }

    private void mostrarErro(Exception e) {
        Toast.makeText(this,
                "Erro: " + e.getLocalizedMessage(),
                Toast.LENGTH_LONG).show();
    }
}
