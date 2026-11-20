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

- Added postman tests for POST /budget/category route using following prompt (4k tokens):

```
add some postman tests for the post /budget/category endpoint to @post-category.postman_collection.json . I specifically want these cases: (1) valid client api key & valid budget belonging to client & valid category name + limit, (2) invalid client api key, (3) valid api key, invalid budget, (4) valid api and budget ids, but budget does not belong to given client, (5) valid api and budget ids but limit given has more then 2 decimal places. Use the format seen in @get-budget.postman_collection.json as a reference
```

- Changed local.postman_environment.json to hold a template for testing using following prompt (2k tokens)

```
okay. i did some online research and the internet says @local.postman_environment.json should store and run environment-specific variables like local server URLs, API keys, and test credentials. HOWEVER as its not good to commit secrets, I would like you to edit the file to make it a template file. So it holds variables and such that @post-category.postman_collection.json and @get-budget.postman_collection.json need to run. Please edit it for me accordingly!
```

- Changed all endpoints as needed after changing POST /budget/category to store a budget with a client's name instead of a client's X-API-KEY (25k tokens)

```
edit @BudgetService.java, @BudgetController.java with the following knowledge: when a new budget is created it stores the client's name instead of the client's api token. Then when we want to check that the given api token (for request) matches the one owning the budget, we must use the client's name to go into the clients.json datastore to get that client's x api token for comparison. Use @clients.json as a reference to how client information is stored. You may also need to edit test cases so they still run as expected
```

- Understanding then fixing merge conflicts in delete-purchases-iter2, understanding then fixing Mockito stubbing warnings (34k tokens). User prompts in order shown below

```
dont edit any code but explain to me why when i run mvn clean verify on the current branch i get the errors i receive. Im trying to figure out what should and should not be kept in the merge conflict resolutions and to do so i need this branch to pass mvn clean verify pre merge

...try to resolve these merge conflicts for me but do NOT push or commit, and also do not run the tests/dont focus on if they pass or not

okay now do not write any code but i am getting these 2 errors: PurchaseServiceTest.rejectsOtherClientsBudget » UnnecessaryStubbing
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at com.collective404.budget.service.PurchaseServiceTest.serviceWithBudgetOwner(PurchaseServiceTest.java:301)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
[ERROR]   PurchaseServiceTest.rejectsOtherClientsBudgetWithForbiddenStatus » UnnecessaryStubbing
Unnecessary stubbings detected.
Clean & maintainable test code requires zero unnecessary code.
Following stubbings are unnecessary (click to navigate to relevant line of code):
  1. -> at com.collective404.budget.service.PurchaseServiceTest.serviceWithBudgetOwner(PurchaseServiceTest.java:301)
Please remove unnecessary stubbings or use 'lenient' strictness. More info: javadoc for UnnecessaryStubbingException class.
**explain why these stubbings are unnecessary, as i did not create these test cases and hence dont fully understand why they do the stubbing they do

then what could i do to these tests to stop this strict stubbing warning w/o just deleting the 2 tests?

okay now you are allowed to change code as you mentioned in the first solution

```

### Tool List
- VSCode Copilot