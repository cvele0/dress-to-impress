import {createBrowserRouter} from "react-router-dom"
import HomePage from "../pages/home/HomePage.tsx";
import LoginPage from "../pages/login/LoginPage.tsx";
import RegisterPage from "../pages/register/RegisterPage.tsx";

export const router = createBrowserRouter([
    {
        path: "/",
        element: <HomePage />
    },
    {
        path: "/login",
        element: <LoginPage />
    },
    {
        path: "/register",
        element: <RegisterPage />
    }
]);