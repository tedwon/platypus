import Head from "next/head";

export default function AppHead() {
    return (
        <Head>
            <title>Platypus</title>
            <meta name="description" content="Platypus is a full-stack CRUD web app with a React frontend and Java + Kotlin backend using Quarkus and Spring Boot, designed for end-to-end development practice with database integration."/>
            <meta name="viewport" content="width=device-width, initial-scale=1"/>
            <link rel="icon" href="/favicon.ico"/>
        </Head>
    );
}