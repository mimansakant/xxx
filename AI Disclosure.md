## AI Disclosure
AI was used in our project for the following:
- Creating automated test suites: prompts were to create code for specified test case branches
- Create the baseline folder structure with file stubs
- Create the original checkstyle.xml, which was then edited to follow that of the Miniproject
- Create BudgetDataStore. Prompted to create a class for file reads and writes, resulting code edited for errors
- Fixing Checkstyle errors using following prompt (33k tokens):
```
i have a checkstyle report in @checkstyle.html. Please edit @BudgetControllerTest.java, @CategoryControllerTest.java, @CategoryUpdateControllerTest.java, @BudgetReportResponse.java, @CategoryRequest.java, @CategoryResponse.java, @CategoryUpdateRequest.java, @Budget.java, @Category.java, @Purchase.java, @BudgetService.java, @BudgetServiceTest.java, @CategoryServiceTest.java @CategoryUpdateServiceTest.java @BudgetDataStore.java @InputValidator.java, @InputValidatorUnitTests.java SUCH THAT they fix these errors (ONLY fix these errors, do not try to otherwise optomize or edit code)
```
- Adding 1 new functionality to version 2 of POST /budget/category using following prompt (10k tokens):
```
change the necessary code in @BudgetService.java, @BudgetController.java, @CategoryRequest.java and @CategoryResponse.java so that in the path where no budgetId is supplied and hence createCategory needs to create a new budget, the client is required to input a budget name in the request body
**only update the code related to createCategory
```
- Converted clientId usage to x-api-key usage following another route's logic in POST /budget/category using following prompt (20k tokens):
```
edit createCategoryWithLock in @BudgetService.java to support the new addition of apiKey in place of clientId. apiKey is what a unique client holds. The logic for checking it should be similar to that seen in buildReportForClient in the same file
```

### Tool List
- VSCode Copilot