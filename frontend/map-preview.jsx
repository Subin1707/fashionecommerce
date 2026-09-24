import React, { useState } from 'react'
import {createRoot} from 'react-dom/client'
import './src/index.css'
import './src/App.css'
import 'leaflet/dist/leaflet.css'
import {DeliveryLocationPicker} from './src/components/cart/DeliveryLocationPicker'
function Preview(){const [value,setValue]=useState({});return <div style={{maxWidth:900,margin:'40px auto',padding:16}}><DeliveryLocationPicker {...value} onChange={(latitude,longitude,address)=>setValue({latitude,longitude,address})}/></div>}
createRoot(document.getElementById('root')).render(<Preview/>);
