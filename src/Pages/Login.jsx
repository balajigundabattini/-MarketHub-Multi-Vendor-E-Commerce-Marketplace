import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../api/client'
import { setAccessToken } from '../api/accessToken'

const Login = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();


    const submitHandler = async (e) => {
        e.preventDefault();
        const userData = {
            email, password
        }

        try {
            const response = await apiClient.post('/auth/login', userData);
            setAccessToken(response.data.accessToken)
            toast.success("Login successfully");
            navigate("/");

        } catch (err) {
            toast.error(err.response?.data?.detail || "Login failed");
        }
    }
  return (
    <div className='d-flex mt-5 '>
            <div className='col-lg-4'></div>
            <div className='col-lg-4 bg-secondary-subtle  border border-2 rounded-2 p-4 lh-lg'>
                <form onSubmit={submitHandler}>
                    <label className='fs-6 fw-bold' htmlFor="login-email">Email</label><br />
                    <input id="login-email" type="email" className='w-75 border rounded-3 ps-2 mb-2' placeholder='Enter Your Email' required onChange={(e) => setEmail(e.target.value)} value={email}/><br />
                    <label className='fs-6 fw-bold' htmlFor="login-password">Password</label><br />
                    <input id="login-password" type="password" className='w-75 border rounded-3 ps-2 mb-2' placeholder='Enter Password' required onChange={(e) => setPassword(e.target.value)} value={password}/><br />
                    <p className='text-center'>Don't have an account? <Link to="/register" className='text-decoration-none'>Register</Link></p>
                    <button className='text-white bg-primary border rounded-4 ps-3 pe-3' type='submit' value="login" >Login</button>
                </form>
            </div>
            <div className='col-lg-4'></div>
        </div>
  )
}

export default Login
