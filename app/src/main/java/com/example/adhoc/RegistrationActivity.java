package com.example.adhoc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {
    private EditText mEmail, mPassword;
    private Button mSubmit;
    private RadioGroup radioGroup, radioGroup2;
    private String selectedOption;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user!=null) {
                    getUserAccType();
                }
            }
        };

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        radioGroup = findViewById(R.id.groupradio);
        mSubmit = findViewById(R.id.submit);
        radioGroup2 = findViewById(R.id.radioGroup);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                final RadioButton radioButton
                        = radioGroup.findViewById(selectedId);
                String selectedOption = (String) radioButton.getText();
                if(selectedOption.equals("Driver"))
                    radioGroup2.setVisibility(View.VISIBLE);
                if(selectedOption.equals("Customer"))
                    radioGroup2.setVisibility(View.GONE);
            }
        });
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();
                if (email.isEmpty() || password.isEmpty() || password.length() < 6) {
                    Toast.makeText(RegistrationActivity.this, "One or more fields invalid", Toast.LENGTH_SHORT).show();
                    return;
                }
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(RegistrationActivity.this,
                            "No answer has been selected",
                            Toast.LENGTH_SHORT)
                            .show();
                }
                else {
                    final RadioButton radioButton = radioGroup.findViewById(selectedId);
                    selectedOption = (String) radioButton.getText();


                    RadioButton radioButton2;
                    int selectedId2;
                    String serviceType = "";
                    if(selectedOption.equals("Driver")) {
                        selectedId2 = radioGroup2.getCheckedRadioButtonId();
                        radioButton2 = radioGroup2.findViewById(selectedId2);
                        selectedOption = (String) radioButton2.getText();
                        serviceType = selectedOption.toString();
                    }
                    final String cabType = serviceType;
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                Toast.makeText(RegistrationActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                            }else{
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users");
                                Toast.makeText(RegistrationActivity.this, selectedOption, Toast.LENGTH_SHORT).show();
                                if (R.id.radia_id1 == radioButton.getId()){
                                    Toast.makeText(RegistrationActivity.this, "Equalizer", Toast.LENGTH_SHORT).show();
                                    current_user_db.child("Customers").child(user_id).setValue(true);
                                }
                                else{
                                    Toast.makeText(RegistrationActivity.this, "Non Equalizer", Toast.LENGTH_SHORT).show();
                                    current_user_db.child("Drivers").child(user_id).setValue(true);

                                    DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id);
                                    Map userInfo = new HashMap();
                                    userInfo.put("name", "");
                                    userInfo.put("phone", "");
                                    userInfo.put("car", "");
                                    userInfo.put("service", cabType);
                                    mDriverDatabase.updateChildren(userInfo);
                                }
                            }
                        }
                    });
                }

            }
        });

    }

    public void getUserAccType() {
        DatabaseReference requestDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        requestDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Intent intent = new Intent(getApplication(), DriverMapActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); /**/
                    finish();
                    startActivity(intent);
                    return;
                } else {
                    Intent intent = new Intent(getApplication(), CustomerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); /**/
                    finish();
                    startActivity(intent);
                    return;

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }
    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
