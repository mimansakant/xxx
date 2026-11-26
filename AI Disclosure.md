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

- Creating the client, alongside debugging it (token counts listed per prompt)
```
Create a client for our service in a seperate folder with the given specifications (do not use readme to understand service, as it is stale): **Bank application: An application for a bank could provide budgeting functionality to its users with accounts at that bank. It could integrate the actual account information, including purchases made through that bank, to automatically update the budget, while also allowing the user to set their own budgets and input their own additional purchases from different sites.

Each bank client instance needs to have their own api key token.

Each instance should offer the following functionality

- Users can login and/or sign up for accounts using username and password
- Users can create bank accounts (credit, debit, whatever else have you)
- Users can create budgets and connect them to a specific bank account
- Users can make purchases from their bank accounts, and when this is done these purchases are reflected in their budget. These are automatically assigned a category based on details from the purchase
- Users can also make purchases not from a bank account, in which case they can merely supply the purchase information and the budget it should be counted towards without connecting it to a specific bank account. In this case it could be for a budget connected to the user alone rather than a specific bank account for said user.
- Users can update the category of any purchase from an account or that they supplied to the application. Other details can be updated for purchases supplied by the user, but not for purchases made from an account
- Users can view their purchase history in total or for each account as well as their budgets with amount used and amount remaining and breakdowns by account or category
- Users can filter any view by date
any capabilities not in the service needs to be implemented in the client. the service should merely act as an api
(98k tokens)
```

```
now add some pre- seeding. For the service this means:

Creating 2-3 more clients. At least 2 should have the same bank name, the other should be a different bank name. All need different id's and apiKey's
Add some budgets associated with these new clients to budgets.json. Lets give each client at least 10 budgets. Note budgets can have the same names BUT must have different ids
Add some categories per budget. Give each budget a random num of categories from 0-5
Add some purchases to each budget/category. Again a random number from 0-5
For the client, pre seeding would entail creating some user and password combinations & connecting them either to pre-seeded budgets as listed above OR to no budgets. Create at least 3 users. Note in readme or some other form of documentation each username and password combination, so that the TA can use them to login as a user on the client without creating a new user at first
(32k tokens)
```

```
now create a .md file in the base project folder with directions on how to run the client and server, so that our TA can run it. Make sure it states that the resources files have been prepopulated, and what they have been populated with
(5k tokens)
```

```
some issues with the client i am seeing rn as i try running it: i am able to login as harbor.alex, however when i try to view budgets i get 'Budget service request failed (HTTP 401)'. When i try to create a new banking account i get 'Cannot read properties of null (reading 'reset')'
(15k tokens)
```

```
okay, now i see new bank account creation and such. was also able to update limit of purchase. but when i try to update limit of category i get this response: {"status":502,"reason":"Budget service could not be reached"}. DO NOT EDIT CODE YET, help me figure out if this is a service issue (service logic is wrong somewhere) or a client side code issue. Because I am able to create new budgets, add new purchases, and delete purchases, and the budget info updates accordingly. Just cannot seem to change th elimit of a pre existing category
(19k tokens)
```

### Tool List
- VSCode Copilot