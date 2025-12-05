function validateBreed() {
    const input = document.getElementById('breedInput');
    const value = input.value.replaceAll(" ", "");
    if (value.length < 3) {
        input.setCustomValidity('Search term must be at least 3 letters and not just spaces');
        return false;
    }
    input.setCustomValidity('');
    return true;
}