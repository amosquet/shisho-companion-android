import requests
url = "https://pocketbase.amcloud.dev/api/collections/shisho_users/records"
payload = {
    "email": "test_user_null@example.com",
    "password": "Password123!",
    "passwordConfirm": "Password123!",
    "discord_id": ""
}
response = requests.post(url, json=payload)
print(response.json())
