var JavaPackages = new JavaImporter(    
  Packages.ray.rage.scene.Light,    
  Packages.java.awt.Color    
);

with (JavaPackages){ 
  function updateLightColor(light, lightTransfer){
    if (lightTransfer == true){
        light.setAmbient(java.awt.Color.darkGray);
    } else {
	light.setAmbient(java.awt.Color.lightGray);
    }
  } 
} 