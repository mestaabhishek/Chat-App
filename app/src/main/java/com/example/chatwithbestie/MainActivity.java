package com.example.chatwithbestie;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MainActivity extends AppCompatActivity {
    private EditText name, phone, email, password;
    private Button register, login, editPicture;
    private DatabaseReference reff, reff1;
    private FirebaseAuth fAuth;
    UserReg user;
    String userID,profilePicUrl;
    private ImageView profilePicture;
    public Uri imageUri,image;
    private StorageReference fstore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.nickNameText);
        phone = findViewById(R.id.mobileNumberText);
        email = findViewById(R.id.emailText);
        password = findViewById(R.id.passwordText);
        register = findViewById(R.id.registerButton);
        login = findViewById(R.id.loginButton);
        editPicture = findViewById(R.id.profileImageEditButton);
        profilePicture = findViewById(R.id.profileImage);

        fstore= FirebaseStorage.getInstance().getReference();
        reff = FirebaseDatabase.getInstance().getReference().child("USERS");
        reff1 = FirebaseDatabase.getInstance().getReference().child("ProfilePic");
        fAuth = FirebaseAuth.getInstance();
        user = new UserReg();

        if (fAuth.getCurrentUser() != null) {
            startActivity(new Intent(MainActivity.this, SelectContactActivity.class));
            finish();
        }

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String firebaseName = name.getText().toString().trim();
                final String firebasePhoneNumber = phone.getText().toString().trim();
                final String firebaseEmail = email.getText().toString().trim();
                final String firebasePassword = password.getText().toString().trim();

                if (TextUtils.isEmpty(firebaseName) || TextUtils.isEmpty(firebasePhoneNumber) || TextUtils.isEmpty(firebaseEmail)
                        || TextUtils.isEmpty(firebasePassword)) {
                    Toast.makeText(MainActivity.this, "Enter All the details..", Toast.LENGTH_SHORT).show();
                } else if(!TextUtils.isEmpty(firebaseName) && !TextUtils.isEmpty(firebasePhoneNumber) && !TextUtils.isEmpty(firebaseEmail)
                        && !TextUtils.isEmpty(firebasePassword)){

                    fAuth.createUserWithEmailAndPassword(firebaseEmail, firebasePassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull final Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                userID = fAuth.getCurrentUser().getUid();
                                user.setUserId(userID);
                                user.setNickName(firebaseName);
                                user.setPhoneNum(firebasePhoneNumber);
                                user.setEmail(firebaseEmail);
                                user.setPassword(firebasePassword);
                                user.setStatus("null");
                                if(imageUri!=null) {
                                    final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                    progressDialog.setTitle("Uploading...");
                                    progressDialog.show();
                                    final StorageReference filepath = fstore.child("ProfileImage").child(userID);
                                    filepath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            progressDialog.dismiss();
                                            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_LONG).show();

                                            filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    UserReg u = new UserReg();
                                                    u.setProfilePic(uri.toString());
                                                    reff1.child(userID).setValue(u);
                                                }
                                            });
                                        }
                                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                            progressDialog.setMessage("Uploaded " + (int) progress + "%");
                                        }
                                    });
                                } else {
                                    //user.setProfilePic()
                                }
                                    reff.child(userID).setValue(user);
                                    updateProfilePic();
                                Toast.makeText(MainActivity.this,"Successfully Registered..! ",Toast.LENGTH_LONG).show();
                                Intent i=new Intent(MainActivity.this,SelectContactActivity.class);
                                startActivity(i);
                                finish();
                            }else {
                                Toast.makeText(MainActivity.this, "Error ! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(MainActivity.this,LoginActivity.class);
                startActivity(i);
            }
        });

        editPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePicture();
            }
        });
        profilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePicture();
            }
        });
    }

    private void updateProfilePic() {
       reff1.child(userID).child("profilePic").addValueEventListener(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot snapshot) {
               if(snapshot.exists()) {
                   profilePicUrl = snapshot.getValue().toString();
                   reff.child(userID).child("profilePic").setValue(profilePicUrl);
               }
           }

           @Override
           public void onCancelled(@NonNull DatabaseError error) {

           }
       });
    }

    private void choosePicture() {
        Intent i=new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(i,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            imageUri=data.getData();
            profilePicture.setImageURI(imageUri);
        }
    }
}
