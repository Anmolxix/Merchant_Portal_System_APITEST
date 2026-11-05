package np.com.moco.Tests;

import io.restassured.response.Response;
import np.com.moco.Client.AddressClient;
import np.com.moco.assertions.ResponseAssertion;
import np.com.moco.base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddressTest extends BaseClass {

    private final AddressClient client = new AddressClient();

    /** Flips one character in the middle of the JWT signature so the signature no longer matches. */
    private String tamper(String token) {
        int i = token.length() - 5;
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + swapped + token.substring(i + 1);
    }

    /**
     * Walks the JSON (lists and objects, however deeply nested) and collects every
     * object that has an "id" field. The province/district nesting keys don't matter.
     */
    private void collectItemsWithId(Object node, List<Map<?, ?>> found) {
        if (node instanceof List) {
            for (Object child : (List<?>) node) {
                collectItemsWithId(child, found);
            }
        } else if (node instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) node;
            if (map.containsKey("id")) {
                found.add(map);
            }
            for (Object child : map.values()) {
                collectItemsWithId(child, found);
            }
        }
    }

    // =============== GET /municipality ===============

    @Test(priority = 1)
    public void getMunicipalitiesTest() {
        Response response = client.getMunicipalities(getAuthToken());

        ResponseAssertion.assertMunicipalityList(response);   // 200, JSON, non-empty list
        ResponseAssertion.assertTokenPresent(response);
    }

    @Test(priority = 2)
    public void getMunicipalitiesWithoutTokenTest() {
        Response response = client.getMunicipalitiesWithoutToken();

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 3)
    public void getMunicipalitiesWithInvalidTokenTest() {
        Response response = client.getMunicipalities("this.is.not.a.valid.token");

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 4)
    public void getMunicipalitiesWithEmptyTokenTest() {
        Response response = client.getMunicipalities("");

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 5)
    public void getMunicipalitiesWithTamperedTokenTest() {
        Response response = client.getMunicipalities(tamper(getAuthToken()));

        ResponseAssertion.assertUnauthorized(response);
    }

    @Test(priority = 6)
    public void getMunicipalitiesEveryItemHasIdAndNameTest() {
        Response response = client.getMunicipalities(getAuthToken());
        ResponseAssertion.assertStatusCode(response, 200);

        List<Map<?, ?>> items = new ArrayList<>();
        collectItemsWithId(response.jsonPath().get("$"), items);

        Assert.assertFalse(items.isEmpty(),
                "No object with an 'id' field found in the response: " + response.asString());

        for (int i = 0; i < items.size(); i++) {
            Object id = items.get(i).get("id");
            Object name = items.get(i).get("name");

            Assert.assertTrue(id != null && !String.valueOf(id).isBlank(),
                    "Item " + i + " has an empty 'id': " + items.get(i));
            Assert.assertTrue(name != null && !String.valueOf(name).isBlank(),
                    "Item " + i + " is missing or has an empty 'name': " + items.get(i));
        }
    }

    @Test(priority = 7)
    public void getMunicipalitiesBodyNotEmptyTest() {
        Response response = client.getMunicipalities(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertBodyNotEmpty(response);
    }

    @Test(priority = 8)
    public void getMunicipalitiesContentTypeTest() {
        Response response = client.getMunicipalities(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertContentTypeContains(response, "application/json");
    }

    @Test(priority = 9)
    public void getMunicipalitiesResponseTimeTest() {
        Response response = client.getMunicipalities(getAuthToken());

        ResponseAssertion.assertStatusCode(response, 200);
        ResponseAssertion.assertResponseTimeBelow(response, 5000);
    }
}