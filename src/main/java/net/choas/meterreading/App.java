package net.choas.meterreading;

import com.sap.apibhub.sdk.client.*;
import com.sap.apibhub.sdk.client.auth.*;
import com.sap.apibhub.sdk.scene_text_recognition_api.api.*;
import com.sap.apibhub.sdk.scene_text_recognition_api.model.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Meter reading with SAP Leonardo.
 * 
 * requires Inference Service for Scene Text Recognition SDK
 * https://api.sap.com/api/scene_text_recognition_api/overview
 * 
 * ... and an API key
 *
 */
public class App {

   public static void main(String[] args) {
      String YOUR_APIBHUB_SANDBOX_APIKEY = "<API Key>";
      String PATH = "/path/to/image/";
      String FILE_NAME = PATH + "ScreenshotMeterReading.png";

      App app = new App();
      try {
         app.meterreading(YOUR_APIBHUB_SANDBOX_APIKEY, PATH, FILE_NAME);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public void meterreading(String apiKey, String path, String fileName) throws IOException, ApiException {
      ResponseOk result = null;

      ApiClient apiClient = Configuration.getDefaultApiClient();
      apiClient.addDefaultHeader("APIKey", apiKey);

      Map<String, Authentication> auths = apiClient.getAuthentications();
      auths.put("APIBHUB_SANDBOX_APIKEY", new ApiKeyAuth("header", "APIKey"));
      auths.put("Oauth2_ClientCredentials", new OAuth());

      SceneTextRecognitionApi apiInstance = new SceneTextRecognitionApi();
      apiInstance.setApiClient(apiClient);

      File files = new File(fileName);
      result = apiInstance.pOSTInferenceSync(files);
      System.out.println(result);
 
      BufferedImage image = ImageIO.read(new File(fileName));
      Graphics2D graph = image.createGraphics();

      String value1 = "";
      String value2 = "";
      BigDecimal y1Value1 = null;
      BigDecimal y1Value2 = null;

      // draw bounding boxes
      for (ExtractedBoxesDetails extractedBoxesDetails : result.getPredictions().get(0).getResults()) {

         graph.setColor(Color.ORANGE);
         graph.setStroke(new BasicStroke(1));

         String text = extractedBoxesDetails.getText();
         if (text.length() >= 6) {

            try {
               Integer.valueOf(text);
               graph.setColor(Color.GREEN);
               graph.setStroke(new BasicStroke(3));

               if (y1Value1 == null) {
                  y1Value1 = extractedBoxesDetails.getBoundingBox().getY1();
                  value1 = text;
               } else {
                  y1Value2 = extractedBoxesDetails.getBoundingBox().getY1();
                  value2 = text;
               }

            } catch (java.lang.NumberFormatException e) {
               // ignore
            }
         }

         drawRect(graph, extractedBoxesDetails.getBoundingBox(), extractedBoxesDetails.getText());
      }

      // draw kWh and serial number
      int kwh;
      int serialNr;
      if (y1Value1.compareTo(y1Value2) == -1) {
         kwh = Integer.valueOf(value1);
         serialNr = Integer.valueOf(value2);
      } else {
         kwh = Integer.valueOf(value2);
         serialNr = Integer.valueOf(value1);
      }

      graph.setColor(Color.GREEN);
      graph.setFont(new Font("Courier", Font.PLAIN, 18));
      graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graph.drawString("kWh: " + kwh / 10 + "." + kwh % 10, 20, 40);
      graph.drawString("Nr.: " + serialNr, 20, 58);

      graph.dispose();

      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);
      frame.add(new JLabel(new ImageIcon(image)));
      frame.pack();

      ImageIO.write(image, "png", new File(path + "meterreading.png"));
   }

   private static void drawRect(Graphics2D graph, ExtractedBoxesDetailsBoundingBox bb, String text) {
      graph.drawLine(bb.getX1().intValue(), bb.getY1().intValue(), bb.getX2().intValue(), bb.getY2().intValue());
      graph.drawLine(bb.getX2().intValue(), bb.getY2().intValue(), bb.getX3().intValue(), bb.getY3().intValue());
      graph.drawLine(bb.getX3().intValue(), bb.getY3().intValue(), bb.getX4().intValue(), bb.getY4().intValue());
      graph.drawLine(bb.getX4().intValue(), bb.getY4().intValue(), bb.getX1().intValue(), bb.getY1().intValue());
   }
}
