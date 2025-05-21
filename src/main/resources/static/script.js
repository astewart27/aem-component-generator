(() => {
    const multifieldComponent = document.querySelector('.multifield-component');
    const multifieldItem = multifieldComponent?.querySelector('.multifield-item');
    const multifieldItemHtml = multifieldItem?.cloneNode(true);
    const deleteMultifieldItemBtn = document.querySelector('.delete-item');
    const addMultifieldItemBtn = document.getElementById('multifield-add-field');
    const generateComponentBtn = document.getElementById('generate-component-btn');
    const form = document.querySelector('form');

    const updateMultifieldHtmlFormInputs = (element) => {
        const elementCopy = element.cloneNode(true);
        const labels = elementCopy.querySelectorAll('label');
        const numItems = multifieldComponent?.querySelectorAll('.multifield-item')?.length;
        for (let i = 0; i < labels.length; i++) {
            const parent = labels[i].parentElement;
            const labelForValue = labels[i].getAttribute('for');
            const input = parent?.querySelectorAll('input[id="'+ labelForValue +'"], select[id="'+ labelForValue +'"]');
            const updatedValue = labelForValue?.replace(/\[(\d+)]/, (_, num) => `[${numItems + 1}]`);
            labels[i].setAttribute('for', updatedValue);
            input[0].setAttribute('name', updatedValue);
            if (input[0].hasAttribute('id')) {
                input[0].setAttribute('id', updatedValue);
            }
        }
        elementCopy.querySelector('.delete-item')?.addEventListener('click', handleDeleteMultifieldItem);
        return elementCopy;
    };

    const handleAddMultifieldItem = async (event) => {
        const button = event.target;
        const parent = button?.closest('.multifield-component');
        const multifieldItemsList = parent?.querySelector('.multifield-items');
        const dynamicallyUpdateHtmlInputs = await updateMultifieldHtmlFormInputs(multifieldItemHtml);
        multifieldItemsList?.append(dynamicallyUpdateHtmlInputs);
    };

    const handleDeleteMultifieldItem = (event) => {
        const numItems = multifieldComponent?.querySelectorAll('.multifield-item')?.length;
        if (numItems > 1) {
            event.target.closest('.multifield-item')?.remove();
        }
    };

    const handleFormData = (componentForm) => {
        const formData = new FormData(componentForm);
        const updatedFormData = new FormData();

        const dialogValuesMap = [];
        for (const [key, value] of formData.entries()) {
            const match = key.match(/(fieldLabelItem|fieldNameItem|fieldTypeItem|isFieldRequiredItem)-\[(\d+)]/);
            if (match) {
                const [, type, index] = match;
                dialogValuesMap[index - 1] = dialogValuesMap[index - 1] || {};
                switch (type) {
                    case 'fieldLabelItem':
                        dialogValuesMap[index - 1]['fieldLabel'] = value;
                        break;
                    case 'fieldNameItem':
                        dialogValuesMap[index - 1]['fieldName'] = value;
                        break;
                    case 'fieldTypeItem':
                        dialogValuesMap[index - 1]['fieldType'] = value;
                        break;
                    case 'isFieldRequiredItem':
                        dialogValuesMap[index - 1]['isFieldRequired'] = value === 'on';
                        break;
                    default:
                        break;
                }
            } else {
                updatedFormData.append(key, value);
            }
        }
        updatedFormData.append('dialogValues', JSON.stringify(dialogValuesMap));
        return updatedFormData;
    };

    const handleFormSubmission = async (event) => {
        event.preventDefault();

        const formData = handleFormData(form);

        try {
            const response = await fetch("/api/generator/form-data", {
                method: "POST",
                body: formData,
            });

            if (!response.ok) {
                console.log('Generate Component Error: ', response.status);
            }

            console.log('Response is OK');
            const json = await response.json();
            console.log('Response: ', json);
        } catch (error) {
            console.error(`Error in request to generate component: ${error.message}`);
        }
    };

    // Event Listeners
    addMultifieldItemBtn?.addEventListener('click', handleAddMultifieldItem);
    deleteMultifieldItemBtn?.addEventListener('click', handleDeleteMultifieldItem);
    generateComponentBtn?.addEventListener('click', handleFormSubmission);
})();