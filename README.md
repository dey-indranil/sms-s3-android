"# sms-s3-android" 
This simple android app, reads sms from your phone and forwards all smss with a keyword to an s3 store.
This was used live and worked fine.
The aws feature which were used were 
1. cognito identity manager
2. s3 transfer utility
`
Pay some attention to the roles configuration and how cognito links up with roles and how to tie it back to the s3 bucket.
Also note that you have to get CognitoCachingCredentialsProvider from Regions.US_EAST_1 (1 of a list of them. e.g. US_WEST_2 doesnt work.)

Further to the s3 store, I had used quicksight to quickly bringup a dashboard for visualization of data which is fairly straightforward.
Feel free to reach out to me if you have any questions.