import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../api/client'
import { clearAccessToken } from '../api/accessToken'

const Register = () => {
    const [displayName, setDisplayName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();


    const submitHandler = async (e) => {
        e.preventDefault();
        const userData = { displayName, email, password }

        try {
            await apiClient.post('/auth/register', userData);
            clearAccessToken()
            toast.success("Check your email to verify your account before logging in.");
            setDisplayName("");
            setEmail("");
            setPassword("");
            navigate("/login");

        } catch (err) {
            toast.error(err.response?.data?.detail || "Registration failed");
        }
    }

    return (
        <div className='d-flex mt-5 '>
            <div className='col-lg-4'></div>
            <div className='col-lg-4 bg-secondary-subtle  border border-2 rounded-2 p-4 lh-lg'>
                <form onSubmit={submitHandler}>
                    <label className='fs-6 fw-bold' htmlFor="register-name">Name</label><br />
                    <input id="register-name" type="text" className='w-75 border rounded-3 ps-2 mb-2' placeholder='Enter Your Name' required onChange={(e) => setDisplayName(e.target.value)} value={displayName} /><br />
                    <label className='fs-6 fw-bold' htmlFor="register-email">Email</label><br />
                    <input id="register-email" type="email" className='w-75 border rounded-3 ps-2 mb-2' placeholder='Enter Your Email' required onChange={(e) => setEmail(e.target.value)} value={email}/><br />
                    <label className='fs-6 fw-bold' htmlFor="register-password">Password</label><br />
                    <input id="register-password" type="password" className='w-75 border rounded-3 ps-2 mb-2' placeholder='Enter Password' required onChange={(e) => setPassword(e.target.value)} value={password}/><br />
                    <p className='text-center'>Already have an account? <Link to="/login" className='text-decoration-none'>Login</Link></p>
                    <button className='text-white bg-primary border rounded-4 ps-3 pe-3' type='submit' value="register" >Register</button>
                </form>
            </div>
            <div className='col-lg-4'></div>
        </div>
    )
}

export default Register
