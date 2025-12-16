--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5
-- Dumped by pg_dump version 17.5

-- Started on 2025-12-16 22:01:41

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 303881)
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- TOC entry 5127 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- TOC entry 889 (class 1247 OID 303910)
-- Name: appointment_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.appointment_status AS ENUM (
    'pending',
    'confirmed',
    'completed',
    'canceled'
);


ALTER TYPE public.appointment_status OWNER TO postgres;

--
-- TOC entry 886 (class 1247 OID 303902)
-- Name: gender_enum; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.gender_enum AS ENUM (
    'male',
    'female',
    'other'
);


ALTER TYPE public.gender_enum OWNER TO postgres;

--
-- TOC entry 883 (class 1247 OID 303893)
-- Name: user_role; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.user_role AS ENUM (
    'admin',
    'doctor',
    'staff',
    'patient'
);


ALTER TYPE public.user_role OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 224 (class 1259 OID 304001)
-- Name: appointments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.appointments (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    patient_id uuid NOT NULL,
    doctor_id uuid,
    service_id uuid,
    branch_id uuid NOT NULL,
    appointment_time timestamp with time zone NOT NULL,
    status character varying(255) DEFAULT 'pending'::public.appointment_status,
    notes character varying(255),
    price_at_booking numeric(38,2) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.appointments OWNER TO postgres;

--
-- TOC entry 238 (class 1259 OID 312886)
-- Name: bill_lines; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bill_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    bill_id uuid NOT NULL,
    product_id uuid,
    quantity integer NOT NULL,
    unit_price numeric(38,2),
    line_amount numeric(38,2),
    created_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.bill_lines OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 312864)
-- Name: bills; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bills (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    total_amount numeric(38,2) NOT NULL,
    status character varying(255) NOT NULL,
    bill_type character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    patient_id uuid,
    creator_id uuid,
    branch_id uuid NOT NULL,
    currency character varying(255) DEFAULT 'VND'::character varying NOT NULL,
    paid_at timestamp with time zone,
    raw_response text,
    recipient_address character varying(255),
    recipient_name character varying(255),
    recipient_phone character varying(255)
);


ALTER TABLE public.bills OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 303919)
-- Name: branches; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.branches (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    branch_name character varying(255) NOT NULL,
    address character varying(255),
    phone_number character varying(255),
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.branches OWNER TO postgres;

--
-- TOC entry 244 (class 1259 OID 321232)
-- Name: chat_message; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat_message (
    id bigint NOT NULL,
    room_id bigint NOT NULL,
    sender_id character varying(255) NOT NULL,
    content text,
    message_type character varying(255) DEFAULT 'TEXT'::character varying,
    metadata text,
    created_at timestamp without time zone DEFAULT now(),
    delivered boolean DEFAULT false,
    read boolean DEFAULT false
);


ALTER TABLE public.chat_message OWNER TO postgres;

--
-- TOC entry 243 (class 1259 OID 321231)
-- Name: chat_message_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chat_message_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chat_message_id_seq OWNER TO postgres;

--
-- TOC entry 5128 (class 0 OID 0)
-- Dependencies: 243
-- Name: chat_message_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chat_message_id_seq OWNED BY public.chat_message.id;


--
-- TOC entry 242 (class 1259 OID 321224)
-- Name: chat_participant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat_participant (
    id bigint NOT NULL,
    room_id bigint NOT NULL,
    user_id character varying(255) NOT NULL,
    role character varying(255),
    joined_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.chat_participant OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 321223)
-- Name: chat_participant_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chat_participant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chat_participant_id_seq OWNER TO postgres;

--
-- TOC entry 5129 (class 0 OID 0)
-- Dependencies: 241
-- Name: chat_participant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chat_participant_id_seq OWNED BY public.chat_participant.id;


--
-- TOC entry 240 (class 1259 OID 321216)
-- Name: chat_room; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat_room (
    id bigint NOT NULL,
    type character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.chat_room OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 321215)
-- Name: chat_room_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chat_room_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chat_room_id_seq OWNER TO postgres;

--
-- TOC entry 5130 (class 0 OID 0)
-- Dependencies: 239
-- Name: chat_room_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chat_room_id_seq OWNED BY public.chat_room.id;


--
-- TOC entry 229 (class 1259 OID 312438)
-- Name: diagnosis_templates; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.diagnosis_templates (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    template_name character varying(255) NOT NULL,
    diagnosis_content text,
    "icd10Code" character varying(50),
    doctor_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    icd10code character varying(255)
);


ALTER TABLE public.diagnosis_templates OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 312649)
-- Name: doctor_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.doctor_profiles (
    user_id uuid NOT NULL,
    specialty character varying(255),
    degree character varying(255)
);


ALTER TABLE public.doctor_profiles OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 303984)
-- Name: inventory; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.inventory (
    product_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    quantity integer DEFAULT 0 NOT NULL,
    expiry_date date,
    last_updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.inventory OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 312621)
-- Name: medical_record_services; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.medical_record_services (
    medical_record_id uuid NOT NULL,
    service_id uuid NOT NULL
);


ALTER TABLE public.medical_record_services OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 304032)
-- Name: medical_records; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.medical_records (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    appointment_id uuid NOT NULL,
    diagnosis text,
    e_signature text,
    is_locked boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    icd10code character varying(255)
);


ALTER TABLE public.medical_records OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 303950)
-- Name: patient_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.patient_profiles (
    user_id uuid NOT NULL,
    date_of_birth date,
    gender character varying(255),
    address character varying(255),
    allergies character varying(255),
    contraindications character varying(255),
    medical_history character varying(255),
    membership_tier character varying(255) DEFAULT 'STANDARD'::character varying,
    points integer DEFAULT 0
);


ALTER TABLE public.patient_profiles OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 312511)
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payments (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    appointment_id uuid,
    amount numeric(38,2) NOT NULL,
    payment_method character varying(255) DEFAULT 'MOMO'::character varying,
    status character varying(255) DEFAULT 'PENDING'::character varying,
    order_id character varying(255),
    transaction_id character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    bill_id uuid,
    paid_at timestamp with time zone
);


ALTER TABLE public.payments OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 304187)
-- Name: prescription_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prescription_items (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    medical_record_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity integer NOT NULL,
    dosage character varying(255),
    notes character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.prescription_items OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 312447)
-- Name: prescription_template_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prescription_template_items (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    template_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity integer NOT NULL,
    dosage character varying(255)
);


ALTER TABLE public.prescription_template_items OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 303962)
-- Name: products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.products (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    product_name character varying(255) NOT NULL,
    description character varying(255),
    price numeric(38,2) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    product_type character varying(255),
    category character varying(255),
    image_url text
);


ALTER TABLE public.products OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 312490)
-- Name: protocol_services; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.protocol_services (
    protocol_id uuid NOT NULL,
    service_id uuid NOT NULL
);


ALTER TABLE public.protocol_services OWNER TO postgres;

--
-- TOC entry 232 (class 1259 OID 312484)
-- Name: protocol_tracking; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.protocol_tracking (
    id uuid NOT NULL,
    completed_sessions integer,
    last_updated_at timestamp(6) with time zone,
    patient_id uuid NOT NULL,
    protocol_service_id uuid NOT NULL,
    start_date timestamp(6) with time zone,
    status character varying(255),
    total_sessions integer NOT NULL,
    CONSTRAINT protocol_tracking_status_check CHECK (((status)::text = ANY ((ARRAY['IN_PROGRESS'::character varying, 'COMPLETED'::character varying])::text[])))
);


ALTER TABLE public.protocol_tracking OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 312473)
-- Name: protocols; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.protocols (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    protocol_name character varying(255) NOT NULL,
    description text,
    total_sessions integer NOT NULL,
    price numeric(38,2) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.protocols OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 304050)
-- Name: reviews; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reviews (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    appointment_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    rating integer NOT NULL,
    comment text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    doctor_id uuid,
    service_id uuid NOT NULL,
    CONSTRAINT reviews_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


ALTER TABLE public.reviews OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 304070)
-- Name: service_materials; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.service_materials (
    service_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity_consumed integer NOT NULL
);


ALTER TABLE public.service_materials OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 303973)
-- Name: services; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.services (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    service_name character varying(255) NOT NULL,
    description character varying(255),
    price numeric(38,2) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.services OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 303930)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    full_name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    phone_number character varying(255),
    password_hash character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    branch_id uuid,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    avatar_public_id character varying(255),
    avatar_url character varying(255)
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 4863 (class 2604 OID 321235)
-- Name: chat_message id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_message ALTER COLUMN id SET DEFAULT nextval('public.chat_message_id_seq'::regclass);


--
-- TOC entry 4861 (class 2604 OID 321227)
-- Name: chat_participant id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_participant ALTER COLUMN id SET DEFAULT nextval('public.chat_participant_id_seq'::regclass);


--
-- TOC entry 4859 (class 2604 OID 321219)
-- Name: chat_room id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_room ALTER COLUMN id SET DEFAULT nextval('public.chat_room_id_seq'::regclass);


--
-- TOC entry 5101 (class 0 OID 304001)
-- Dependencies: 224
-- Data for Name: appointments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.appointments (id, patient_id, doctor_id, service_id, branch_id, appointment_time, status, notes, price_at_booking, created_at, updated_at) FROM stdin;
b4573eec-823f-4784-b5bb-ad6cf49c6649	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-25 02:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 16:05:40.988225+07	2025-10-14 16:05:40.988225+07
c363ef2d-04f8-4112-9f37-baa8e9477308	2b3b31ee-cc2e-449c-81dd-e19dbac3a837	44ec66f7-8ab0-4e65-b47e-f11df325d938	91fd3ee3-060f-4957-b7f9-b983e01c4d4d	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-10 10:00:00+07	CONFIRMED	Bệnh nhân có tiền sử dị ứng penicillin.	250000.00	2025-10-02 11:07:15.103413+07	2025-10-02 14:58:18.216064+07
ef8ee174-71fb-4286-a50e-62416be3449d	5b69f9a8-cf5b-4b7f-9f8f-30996f52452b	44ec66f7-8ab0-4e65-b47e-f11df325d938	91fd3ee3-060f-4957-b7f9-b983e01c4d4d	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-10 10:00:00+07	PENDING	\N	250000.00	2025-10-01 11:47:56.686082+07	2025-10-01 11:47:56.686082+07
c4121f6a-959a-47ea-b609-8e66dd61e3e2	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-25 04:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 16:11:39.281781+07	2025-10-14 16:11:39.281781+07
73f18a1d-6209-4c5b-a337-dfe370b64d66	5b69f9a8-cf5b-4b7f-9f8f-30996f52452b	44ec66f7-8ab0-4e65-b47e-f11df325d938	91fd3ee3-060f-4957-b7f9-b983e01c4d4d	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-10 10:00:00+07	COMPLETED	Bệnh nhân có tiền sử dị ứng penicillin.	250000.00	2025-10-01 21:15:59.405745+07	2025-10-06 22:35:16.481694+07
a9493dac-c3d8-4f3d-a35d-6f723ec73a22	64ffbdb3-d785-4c50-a236-393b89f821e2	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-07 07:00:00+07	CONFIRMED	Không có.	500000.00	2025-10-06 23:08:53.756557+07	2025-10-06 23:09:26.332343+07
e1fc60b8-1999-4c4e-823b-54776f79c723	a189e205-4a21-496b-948d-f610ffc08f23	b0d87dbd-fe60-463a-b7ed-ed53770082a7	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-07 07:00:00+07	PENDING	Không có.	500000.00	2025-10-06 23:13:05.352011+07	2025-10-06 23:13:05.352011+07
95132827-31ab-4b7e-a215-3b3b24589947	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-07 09:00:00+07	PENDING	Không có.	500000.00	2025-10-06 23:15:41.129638+07	2025-10-06 23:15:41.129638+07
1e97e333-0cf0-4cb1-9bec-02220a240dc4	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-07 10:00:00+07	CONFIRMED	Không có.	500000.00	2025-10-07 00:16:47.822268+07	2025-10-07 00:17:32.784121+07
ae9b61b7-b5cf-4b59-9595-3f31fd71c32a	7850fd0a-3a80-4afd-9de2-c16136880f3d	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-10 10:00:00+07	CONFIRMED	Không có.	500000.00	2025-10-03 10:35:03.814977+07	2025-10-07 01:28:54.843106+07
25720c73-6213-45a7-8a60-b1d99e4379b9	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-07 13:00:00+07	PENDING	Không có.	500000.00	2025-10-07 01:39:36.8208+07	2025-10-07 01:39:36.8208+07
e978e91d-dd53-4596-91a1-418b0d5591e7	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-25 06:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 16:14:55.240061+07	2025-10-14 16:14:55.240061+07
3a486dde-c882-4873-861b-63220f3d5a29	2b3b31ee-cc2e-449c-81dd-e19dbac3a837	44ec66f7-8ab0-4e65-b47e-f11df325d938	91fd3ee3-060f-4957-b7f9-b983e01c4d4d	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-10 10:00:00+07	COMPLETED	Bệnh nhân có tiền sử dị ứng penicillin.	250000.00	2025-10-02 14:42:50.949881+07	2025-10-07 10:58:43.623758+07
1834a319-f9bc-4a52-b811-f82619241f13	2b3b31ee-cc2e-449c-81dd-e19dbac3a837	44ec66f7-8ab0-4e65-b47e-f11df325d938	91fd3ee3-060f-4957-b7f9-b983e01c4d4d	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-08 12:30:00+07	PENDING	Bệnh nhân yêu cầu đổi giờ hẹn.	250000.00	2025-10-03 00:12:50.492754+07	2025-10-08 09:58:03.536356+07
635d9680-9f0c-40c3-a359-7626cdb594a6	7850fd0a-3a80-4afd-9de2-c16136880f3d	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-10 10:00:00+07	CANCELED	Không có.	500000.00	2025-10-03 10:39:38.060373+07	2025-10-12 10:16:15.534788+07
295edb4d-ed0c-46b3-9eea-ee5713fa97fa	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-14 15:00:00+07	PENDING	test thanh toán	500000.00	2025-10-14 11:46:20.876522+07	2025-10-14 11:46:20.876522+07
77033994-970f-4292-b259-2854d7849fe8	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-14 18:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 14:42:40.694393+07	2025-10-14 14:42:40.694393+07
2d6e8320-5118-43bb-a377-99fda0660334	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-14 20:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 14:43:27.509311+07	2025-10-14 14:43:27.509311+07
94357ea5-5256-440c-8748-f5a8905fd4d0	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-16 20:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 14:57:49.056478+07	2025-10-14 14:57:49.056478+07
145138b9-3233-41a0-be13-3857be38bf2c	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-16 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:01:38.959977+07	2025-10-14 15:01:38.959977+07
04f10759-5ed4-4678-a344-feaf2c5c64e5	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-17 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:02:41.228047+07	2025-10-14 15:02:41.228047+07
e1a17517-b4f1-485e-a20b-7d72678f7e7c	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-18 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:05:14.264262+07	2025-10-14 15:05:14.264262+07
d3a05bcd-755b-4c8e-acec-acf29cc88d29	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-19 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:10:41.450633+07	2025-10-14 15:10:41.450633+07
9a3a4bbd-5a68-4147-8eb2-c1e0f8ed7bea	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-20 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:15:56.707692+07	2025-10-14 15:15:56.707692+07
312c96fd-411f-4196-a7a2-d77b0bbe01e5	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-21 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:16:58.417634+07	2025-10-14 15:16:58.417634+07
7d01e4eb-5d7f-4418-803a-73d4b59ad354	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-22 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:19:33.195644+07	2025-10-14 15:19:33.195644+07
853acfca-6981-4d10-b7f0-d7041b76a7b3	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-23 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:35:24.515928+07	2025-10-14 15:35:24.515928+07
5722fd8c-2ba2-4594-ae7d-3cb0eba94210	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-24 22:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:37:41.37357+07	2025-10-14 15:37:41.37357+07
5a500b49-f1c1-4598-9479-5f655bb376d6	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-24 02:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 15:43:35.148548+07	2025-10-14 15:43:35.148548+07
eec99efa-04fc-4894-8f6c-c60a1ea6ca83	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-26 06:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 16:26:19.798831+07	2025-10-14 16:26:19.798831+07
976b94c2-03ad-43eb-b22b-176f4279a12b	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-26 08:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 17:07:21.676017+07	2025-10-14 17:07:21.676017+07
a9c1d4cf-6f5e-448c-950f-63518b9b7ca6	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-27 08:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 17:09:05.894623+07	2025-10-14 17:09:05.894623+07
01f40ef5-7ecf-47c3-a845-d10335f5b2de	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-28 08:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-15 10:02:38.213863+07	2025-10-15 10:02:38.213863+07
d66d44a0-3a4e-452b-beda-8236b215f7b3	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-29 08:00:00+07	PENDING	test thanh toán	500000.00	2025-10-15 10:48:29.710393+07	2025-10-15 10:48:29.710393+07
46e9d64b-c150-495b-9d1b-fc6b34ccf52c	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-29 08:00:00+07	CONFIRMED	test thanh toán	500000.00	2025-10-16 15:30:03.638263+07	2025-10-16 16:10:26.517535+07
43c59d44-4648-452e-ac9a-acdccd44d57a	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-29 08:00:00+07	PENDING	test thanh toán	500000.00	2025-10-16 16:32:46.769127+07	2025-10-16 16:32:46.769127+07
55238102-01ee-4fb5-bc76-1c5d589728ff	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-29 12:00:00+07	PENDING	test thanh toán	500000.00	2025-10-16 16:40:31.872849+07	2025-10-16 16:40:31.872849+07
99eb63fa-d8f4-452d-a018-ca83e5513c6b	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-29 14:00:00+07	CONFIRMED	test thanh toán	500000.00	2025-10-16 16:47:33.394101+07	2025-10-17 16:16:51.592251+07
a374505e-9eb3-4bc2-a134-72024f7e0fc4	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-29 16:00:00+07	PENDING	test thanh toán	500000.00	2025-10-18 11:37:50.693719+07	2025-10-18 11:37:50.693719+07
a0c204f9-815c-4c2e-8378-50d7a961968e	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-29 18:00:00+07	PENDING	test thanh toán	500000.00	2025-10-18 21:07:09.4661+07	2025-10-18 21:07:09.4661+07
53fc7976-ceba-44a9-8c26-742f9cc159ee	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-22 09:00:00+07	PENDING_PAYMENT	test đặt lịch	150000.00	2025-10-22 15:05:03.293138+07	2025-10-22 15:05:03.293138+07
53550399-6c48-4a93-93ee-7ffb83f0227e	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-29 22:00:00+07	CONFIRMED	test thanh toán	500000.00	2025-10-18 21:56:25.257908+07	2025-10-18 21:57:23.302551+07
92cecd79-2ba1-4b13-8b32-3e880bfdb77f	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-29 20:00:00+07	COMPLETED	test thanh toán	500000.00	2025-10-18 21:22:20.178575+07	2025-10-21 10:25:58.196875+07
78cdb2ca-8952-4d72-9d31-bd4ed0ac4d75	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-22 14:00:00+07	PENDING	test đặt lịch	150000.00	2025-10-22 15:22:09.351468+07	2025-10-22 15:22:09.351468+07
a21b3bd6-8a59-4011-9163-50a3a5d5c2e5	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-22 15:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-10-22 15:28:22.028088+07	2025-10-22 15:32:35.278026+07
599a8ac1-cd52-48dc-b5c6-a46b9234fb20	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-02 15:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-10-31 14:28:53.431835+07	2025-10-31 14:34:05.424908+07
ec7f01b3-4227-4977-835f-52c5a70f01e0	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-06 15:00:00+07	PENDING	test đặt lịch	150000.00	2025-11-05 23:38:42.162207+07	2025-11-05 23:38:42.162207+07
7196d830-db59-40ed-a94d-053687f9b1aa	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-10-15 20:00:00+07	PENDING_PAYMENT	test thanh toán	500000.00	2025-10-14 14:46:19.337168+07	2025-10-14 14:46:19.337168+07
f205b6e6-48da-4a34-9ccd-b50bc1865795	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-22 15:00:00+07	PENDING	test đặt lịch	150000.00	2025-11-21 00:44:53.503638+07	2025-11-21 00:44:53.503638+07
9288ef73-4b7a-485c-9213-a188cc891836	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-23 15:00:00+07	PENDING	test đặt lịch	150000.00	2025-11-21 10:37:32.43052+07	2025-11-21 10:37:32.43052+07
ff12dac9-bc25-4a48-ba33-cdaad43abb14	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-24 15:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-21 10:40:50.20219+07	2025-11-21 10:42:04.528679+07
bea11f55-65cc-4a16-a145-3eef3025c094	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-25 15:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-21 10:44:19.988122+07	2025-11-21 10:45:15.270159+07
424e67a8-380d-4ef2-8a97-fb92110c65eb	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-26 15:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-21 14:24:13.938909+07	2025-11-21 14:25:24.488369+07
30dd6b1e-e567-4f4e-a850-2b5e851dfe15	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-27 15:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-21 14:45:16.75278+07	2025-11-21 14:46:29.956323+07
3acbdf35-bef7-493c-89e5-1d5db9af6bb5	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-27 14:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-21 14:49:57.885601+07	2025-11-21 14:51:01.731325+07
a04a61fe-775c-49cb-abac-0ca2b8bf2867	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-27 07:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-21 14:52:27.341594+07	2025-11-21 14:53:29.833451+07
e45e9401-54e2-406e-96ba-9ae1d582b441	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-28 07:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-24 20:33:24.014379+07	2025-11-24 20:35:40.414309+07
cc1cdd1b-05e6-481d-af25-7d53e92fe3fd	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-29 07:00:00+07	CONFIRMED	test đặt lịch	150000.00	2025-11-24 21:11:38.173329+07	2025-11-24 21:34:20.628093+07
6f9b3d8c-7773-4670-b531-893701459d89	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-29 09:00:00+07	PAID_SERVICE	test đặt lịch	150000.00	2025-11-25 09:19:23.489452+07	2025-11-25 09:23:34.749876+07
c2f12c38-7bf3-4750-bbd8-26a888d39ee4	a189e205-4a21-496b-948d-f610ffc08f23	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-11-29 10:00:00+07	PENDING	test đặt lịch	150000.00	2025-11-25 09:50:31.007767+07	2025-11-25 09:50:31.007767+07
48b5a5c8-1d3a-4c78-8f31-c043845dd1a9	a189e205-4a21-496b-948d-f610ffc08f23	b0d87dbd-fe60-463a-b7ed-ed53770082a7	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-11 09:00:00+07	CONFIRMED	zxcvxzcv	150000.00	2025-12-08 17:01:44.303607+07	2025-12-08 17:04:29.590866+07
786a6785-982d-4bf3-a03b-f9cd3eb1fbbf	a189e205-4a21-496b-948d-f610ffc08f23	b0d87dbd-fe60-463a-b7ed-ed53770082a7	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-11 09:15:00+07	COMPLETED	zcxvxzcv	150000.00	2025-12-10 10:57:04.691444+07	2025-12-10 11:56:58.707971+07
79132aa7-9772-42a1-aebf-1876a05f3c4f	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-10 08:00:00+07	CONFIRMED	h	150000.00	2025-12-10 12:05:02.689242+07	2025-12-10 12:05:37.058172+07
0b54e460-5526-4078-b752-32fa91ba77a0	a189e205-4a21-496b-948d-f610ffc08f23	db969247-58a1-447d-a247-88582eedbfcd	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-14 08:00:00+07	PENDING	khó	150000.00	2025-12-14 16:04:08.483987+07	2025-12-14 16:04:08.483987+07
5e2d8854-815e-43e0-8db6-94142c147ca6	a189e205-4a21-496b-948d-f610ffc08f23	44ec66f7-8ab0-4e65-b47e-f11df325d938	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-14 08:00:00+07	PENDING	đau	150000.00	2025-12-14 16:04:35.845885+07	2025-12-14 16:04:35.845885+07
f7f251c7-27c6-4bf2-b4b4-646c8c8224d8	a189e205-4a21-496b-948d-f610ffc08f23	b0d87dbd-fe60-463a-b7ed-ed53770082a7	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	2025-12-16 08:00:00+07	PAID_SERVICE	ốm	150000.00	2025-12-16 17:01:43.175671+07	2025-12-16 17:05:02.955267+07
\.


--
-- TOC entry 5115 (class 0 OID 312886)
-- Dependencies: 238
-- Data for Name: bill_lines; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bill_lines (id, bill_id, product_id, quantity, unit_price, line_amount, created_at) FROM stdin;
b57b8c97-a03e-4407-a7d8-415dfcef9fea	b7e6c4db-b1c8-4fbb-b25f-24cfb4932361	4c0b79b9-9431-4ebb-8119-e33079e927f9	2	30000.00	60000.00	2025-11-21 16:11:22.803417+07
7313fb72-8b06-4306-a6d2-27ba36d97f5e	b7e6c4db-b1c8-4fbb-b25f-24cfb4932361	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	2	10000.00	20000.00	2025-11-21 16:11:22.806418+07
940f87cc-4fe0-4942-a444-ab2e9014f886	8fdd2898-f54f-4668-a72d-6e5cacf847f9	4c0b79b9-9431-4ebb-8119-e33079e927f9	2	30000.00	60000.00	2025-11-21 17:11:41.669932+07
034a5b5b-b665-4d29-9558-edadb71192ac	8fdd2898-f54f-4668-a72d-6e5cacf847f9	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	2	10000.00	20000.00	2025-11-21 17:11:41.674485+07
2f53fcce-f3a7-4d90-9b62-e5604f8a123b	9f99f17f-aabb-4c9c-9dec-4f441464493e	4c0b79b9-9431-4ebb-8119-e33079e927f9	5	30000.00	150000.00	2025-11-26 14:30:41.111098+07
f1bb4e2d-e5ca-4991-8c68-68ac98fdd604	9f99f17f-aabb-4c9c-9dec-4f441464493e	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	2	10000.00	20000.00	2025-11-26 14:30:41.115626+07
fc680354-238a-4b2c-9a88-b2520f4c8fd5	730ea8b4-8697-4b1d-91fa-3fc03a15a60c	4c0b79b9-9431-4ebb-8119-e33079e927f9	5	30000.00	150000.00	2025-11-26 14:32:58.662769+07
621ff6f3-c89e-48aa-87e8-d109991ebc39	730ea8b4-8697-4b1d-91fa-3fc03a15a60c	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	2	10000.00	20000.00	2025-11-26 14:32:58.665214+07
\.


--
-- TOC entry 5114 (class 0 OID 312864)
-- Dependencies: 237
-- Data for Name: bills; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bills (id, total_amount, status, bill_type, created_at, updated_at, patient_id, creator_id, branch_id, currency, paid_at, raw_response, recipient_address, recipient_name, recipient_phone) FROM stdin;
f3de7544-d218-4cbe-b34d-15cfb809be07	150000.00	PENDING	SERVICE_PAYMENT	2025-11-21 14:25:10.511924+07	2025-11-21 14:25:10.511924+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	\N	\N	\N	\N	\N
ad7c3db2-78c0-4b98-bd8f-dc5d624ff2c5	750000.00	PENDING	SERVICE_PAYMENT	2025-11-21 14:31:03.479541+07	2025-11-21 14:31:03.479541+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	\N	\N	\N	\N	\N
10497358-6d3c-4191-bc0c-601e2fd8d5f1	150000.00	PENDING	SERVICE_PAYMENT	2025-11-21 14:46:16.489801+07	2025-11-21 14:46:16.489801+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	\N	\N	\N	\N	\N
8afc9990-60ef-4425-ad47-9a1ac9cc1f36	150000.00	PAID	APPOINTMENT_PAYMENT	2025-11-21 14:53:16.240103+07	2025-11-21 14:53:16.310811+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-21 14:53:16.307811+07	\N	\N	\N	\N
07a954f7-036e-4707-bb7f-26cae2a305cb	750000.00	PAID	SERVICE_PAYMENT	2025-11-21 14:57:25.626229+07	2025-11-21 14:57:25.635223+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-21 14:57:25.635223+07	\N	\N	\N	\N
b7e6c4db-b1c8-4fbb-b25f-24cfb4932361	80000.00	PAID	DRUG_PAYMENT	2025-11-21 16:11:22.778598+07	2025-11-21 16:27:24.269165+07	a189e205-4a21-496b-948d-f610ffc08f23	1ceff163-ff36-4709-9af9-44292a9a5418	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-21 16:27:24.26616+07	\N	\N	\N	\N
8fdd2898-f54f-4668-a72d-6e5cacf847f9	80000.00	PAID	DRUG_PAYMENT	2025-11-21 17:11:41.663899+07	2025-11-21 17:16:30.537023+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-21 17:16:30.537024+07	\N	\N	\N	\N
50bb6c07-0cfc-4021-8d31-7241f32ad6f7	150000.00	PAID	APPOINTMENT_PAYMENT	2025-11-24 20:35:26.331721+07	2025-11-24 20:35:26.409105+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-24 20:35:26.406747+07	\N	\N	\N	\N
17eaa0b3-9913-433a-9f92-6e0397a12051	150000.00	PAID	APPOINTMENT_PAYMENT	2025-11-24 21:12:25.345664+07	2025-11-24 21:12:25.421089+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-24 21:12:25.41909+07	\N	\N	\N	\N
f7e4abae-b2c7-4e62-9147-63c895f3d7fc	750000.00	PAID	SERVICE_PAYMENT	2025-11-24 21:34:06.413083+07	2025-11-24 21:34:06.424103+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-24 21:34:06.423102+07	\N	\N	\N	\N
8773c6ea-e721-4d8c-b441-6c999b855281	150000.00	PAID	APPOINTMENT_PAYMENT	2025-11-25 09:20:51.136663+07	2025-11-25 09:20:51.207817+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-25 09:20:51.204818+07	\N	\N	\N	\N
077f0d41-d81d-4353-aa84-31adacdcc7f1	750000.00	PAID	SERVICE_PAYMENT	2025-11-25 09:23:20.731723+07	2025-11-25 09:23:20.741848+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-11-25 09:23:20.741848+07	\N	\N	\N	\N
9f99f17f-aabb-4c9c-9dec-4f441464493e	170000.00	PENDING	DRUG_PAYMENT	2025-11-26 14:30:41.089541+07	2025-11-26 14:30:41.089541+07	\N	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	\N	\N	Ngoc Khanh	Minh	0123123123
730ea8b4-8697-4b1d-91fa-3fc03a15a60c	170000.00	PENDING	DRUG_PAYMENT	2025-11-26 14:32:58.655182+07	2025-11-26 14:32:58.655182+07	\N	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	\N	\N	Ngoc Khanh	Minh	0123123123
790fde3a-721c-49bf-a6ce-2b5ab3c6a5a1	150000.00	PAID	APPOINTMENT_PAYMENT	2025-12-08 17:04:01.150196+07	2025-12-08 17:04:01.207338+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-12-08 17:04:01.203337+07	\N	\N	\N	\N
027062ea-0686-42f6-a27f-eeb684a6b2f8	150000.00	PAID	APPOINTMENT_PAYMENT	2025-12-10 10:57:42.805548+07	2025-12-10 10:57:42.888009+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-12-10 10:57:42.885005+07	\N	\N	\N	\N
7bd232a8-297c-4d2d-8718-86c97f3ef155	550000.00	PAID	SERVICE_PAYMENT	2025-12-10 11:55:51.431393+07	2025-12-10 11:55:51.462959+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-12-10 11:55:51.459957+07	\N	\N	\N	\N
65ef71e0-fdfa-4766-9fc7-ee7de90b00df	150000.00	PAID	APPOINTMENT_PAYMENT	2025-12-10 12:05:23.835912+07	2025-12-10 12:05:23.850429+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-12-10 12:05:23.84943+07	\N	\N	\N	\N
08129198-60b1-482a-844d-cc8066d28368	150000.00	PAID	APPOINTMENT_PAYMENT	2025-12-16 17:02:19.893452+07	2025-12-16 17:02:19.971911+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-12-16 17:02:19.968913+07	\N	\N	\N	\N
a05c91ad-330e-4928-aa38-5dbad00cc933	250000.00	PAID	SERVICE_PAYMENT	2025-12-16 17:04:50.224902+07	2025-12-16 17:04:50.240047+07	a189e205-4a21-496b-948d-f610ffc08f23	\N	649847b6-3def-4a78-bbe1-f480b4bbbfaf	VND	2025-12-16 17:04:50.240048+07	\N	\N	\N	\N
\.


--
-- TOC entry 5095 (class 0 OID 303919)
-- Dependencies: 218
-- Data for Name: branches; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.branches (id, branch_name, address, phone_number, is_active, created_at, updated_at) FROM stdin;
649847b6-3def-4a78-bbe1-f480b4bbbfaf	Chi nhánh Trung tâm	123 Đường ABC, Quận 1, TPHCM	02838123456	t	2025-10-01 11:46:26.33198+07	2025-10-01 11:46:26.33198+07
f7816c42-9d8c-4ac5-bd89-aea043049ff4	Chi nhánh phụ 1	chi nhánh phụ 1	02838123456	t	2025-10-04 22:57:18.316592+07	2025-10-04 22:57:18.316592+07
3ba3b1b6-afb5-46e2-8769-65043df571d9	Chi nhánh phụ 2	chi nhánh phụ 2	02838123426	t	2025-10-05 00:19:44.407678+07	2025-10-05 00:19:44.407678+07
\.


--
-- TOC entry 5121 (class 0 OID 321232)
-- Dependencies: 244
-- Data for Name: chat_message; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat_message (id, room_id, sender_id, content, message_type, metadata, created_at, delivered, read) FROM stdin;
22	2	a189e205-4a21-496b-948d-f610ffc08f23	hello bro	TEXT	\N	2025-12-08 00:13:45.954239	f	f
23	2	a189e205-4a21-496b-948d-f610ffc08f23	hello bro	TEXT	\N	2025-12-08 00:24:38.237165	f	f
24	2	44ec66f7-8ab0-4e65-b47e-f11df325d938	hj	TEXT	\N	2025-12-08 00:24:52.339877	f	f
25	2	a189e205-4a21-496b-948d-f610ffc08f23	hello bro	TEXT	\N	2025-12-08 00:27:11.577631	f	f
26	2	44ec66f7-8ab0-4e65-b47e-f11df325d938	hj	TEXT	\N	2025-12-08 00:27:23.758554	f	f
27	2	a189e205-4a21-496b-948d-f610ffc08f23	1234	TEXT	\N	2025-12-08 00:29:31.409332	f	f
28	2	44ec66f7-8ab0-4e65-b47e-f11df325d938	zzz	TEXT	\N	2025-12-08 00:29:42.788174	f	f
62	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	helo bro	TEXT	\N	2025-12-08 16:28:19.646193	f	f
63	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	hi	TEXT	\N	2025-12-08 16:28:27.743349	f	f
29	2	a189e205-4a21-496b-948d-f610ffc08f23	hj	TEXT	\N	2025-12-08 01:28:56.917505	t	t
64	48	a189e205-4a21-496b-948d-f610ffc08f23	chào bác sĩ	TEXT	\N	2025-12-08 16:29:09.446725	f	f
65	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	he	TEXT	\N	2025-12-08 16:30:17.56441	f	f
30	2	a189e205-4a21-496b-948d-f610ffc08f23	helo boy	TEXT	\N	2025-12-08 01:29:26.867199	t	t
31	40	db969247-58a1-447d-a247-88582eedbfcd	z	TEXT	\N	2025-12-08 15:37:28.372736	f	f
32	43	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	123	TEXT	\N	2025-12-08 15:38:32.19726	f	f
33	43	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	xzczxc	TEXT	\N	2025-12-08 15:38:34.937514	f	f
34	43	db969247-58a1-447d-a247-88582eedbfcd	zxcv	TEXT	\N	2025-12-08 15:41:21.873535	f	f
35	43	db969247-58a1-447d-a247-88582eedbfcd	ádf	TEXT	\N	2025-12-08 15:41:30.949767	f	f
36	43	db969247-58a1-447d-a247-88582eedbfcd	vvvvvv	TEXT	\N	2025-12-08 15:41:54.440836	f	f
37	43	db969247-58a1-447d-a247-88582eedbfcd	ô	TEXT	\N	2025-12-08 15:41:58.817645	f	f
38	43	db969247-58a1-447d-a247-88582eedbfcd	hi	TEXT	\N	2025-12-08 15:42:01.95792	f	f
39	43	db969247-58a1-447d-a247-88582eedbfcd	zzzzzzzzzzzzzzzzzzzzzzzzz	TEXT	\N	2025-12-08 15:42:54.031639	f	f
40	44	db969247-58a1-447d-a247-88582eedbfcd	helo bro	TEXT	\N	2025-12-08 15:44:52.376531	f	f
41	45	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	hi bro	TEXT	\N	2025-12-08 15:46:08.024623	f	f
42	45	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	helo	TEXT	\N	2025-12-08 15:46:22.00789	f	f
43	45	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	ok bạn bị sao	TEXT	\N	2025-12-08 15:46:29.072321	f	f
44	45	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	tôi đau đầu	TEXT	\N	2025-12-08 15:46:32.603019	f	f
45	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	Z	TEXT	\N	2025-12-08 15:54:07.878325	f	f
46	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	Z	TEXT	\N	2025-12-08 15:54:14.445233	f	f
47	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	XZCV	TEXT	\N	2025-12-08 15:54:18.914262	f	f
48	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	zxcv	TEXT	\N	2025-12-08 15:54:28.625235	f	f
49	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	vvvvv	TEXT	\N	2025-12-08 15:54:31.985009	f	f
50	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	vvvvvvvvvvvvvvvvvvv	TEXT	\N	2025-12-08 15:54:41.343733	f	f
51	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	aaaaaaaaaaaa	TEXT	\N	2025-12-08 15:54:44.575834	f	f
52	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	zxcv	TEXT	\N	2025-12-08 16:07:06.764389	f	f
53	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	hjhjhj	TEXT	\N	2025-12-08 16:07:12.269959	f	f
54	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	tốt e	TEXT	\N	2025-12-08 16:07:15.182171	f	f
55	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	ok e	TEXT	\N	2025-12-08 16:07:17.003815	f	f
56	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	zxc	TEXT	\N	2025-12-08 16:11:21.802473	f	f
57	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	vv	TEXT	\N	2025-12-08 16:11:26.105591	f	f
58	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	z	TEXT	\N	2025-12-08 16:11:43.22204	f	f
59	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	zxc	TEXT	\N	2025-12-08 16:18:05.345311	f	f
60	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	v	TEXT	\N	2025-12-08 16:18:14.685784	f	f
61	46	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	vzcxvzxcv	TEXT	\N	2025-12-08 16:18:18.345072	f	f
66	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	chàoi	TEXT	\N	2025-12-08 16:30:25.387266	f	f
67	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	ô	TEXT	\N	2025-12-08 16:30:29.222582	f	f
68	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	zxc	TEXT	\N	2025-12-08 16:33:35.498409	f	f
69	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	123	TEXT	\N	2025-12-08 16:33:44.885304	f	f
70	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	zxv	TEXT	\N	2025-12-08 16:34:48.787575	f	f
71	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	123123	TEXT	\N	2025-12-08 16:35:06.126527	f	f
72	48	a189e205-4a21-496b-948d-f610ffc08f23	alo a zai	TEXT	\N	2025-12-08 16:38:31.814192	f	f
73	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	t đây	TEXT	\N	2025-12-08 16:43:48.812411	f	f
74	48	a189e205-4a21-496b-948d-f610ffc08f23	ok azia	TEXT	\N	2025-12-08 16:43:55.283543	f	t
75	48	a189e205-4a21-496b-948d-f610ffc08f23	ehheeheh	TEXT	\N	2025-12-08 16:44:02.539639	f	t
76	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	ok bro	TEXT	\N	2025-12-08 16:44:11.290926	f	f
77	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	hiii	TEXT	\N	2025-12-09 09:24:25.555016	f	t
78	48	a189e205-4a21-496b-948d-f610ffc08f23	zxcv	TEXT	\N	2025-12-09 09:24:38.424396	f	f
79	48	a189e205-4a21-496b-948d-f610ffc08f23	hj	TEXT	\N	2025-12-09 09:24:40.564328	f	f
80	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	ô	TEXT	\N	2025-12-09 09:24:55.448863	f	f
81	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	hi	TEXT	\N	2025-12-09 09:24:56.534894	f	f
82	48	a189e205-4a21-496b-948d-f610ffc08f23	chào bác sĩ	TEXT	\N	2025-12-09 09:25:27.758275	f	f
83	48	a189e205-4a21-496b-948d-f610ffc08f23	chào cô	TEXT	\N	2025-12-10 00:31:49.286984	f	f
84	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	chào anh có vấn đề gì thế	TEXT	\N	2025-12-10 00:32:06.289774	f	t
85	48	a189e205-4a21-496b-948d-f610ffc08f23	tôi chán cô lắm	TEXT	\N	2025-12-10 00:32:11.322489	f	t
86	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	ok kệ anh	TEXT	\N	2025-12-10 00:32:15.713299	f	t
87	48	a189e205-4a21-496b-948d-f610ffc08f23	ok bro	TEXT	\N	2025-12-10 00:32:29.101601	f	t
88	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	nnao	TEXT	\N	2025-12-10 00:32:39.98763	f	f
89	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	sao zij	TEXT	\N	2025-12-10 00:32:49.725785	f	t
90	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	chạ sao	TEXT	\N	2025-12-10 00:32:54.32572	f	t
91	48	a189e205-4a21-496b-948d-f610ffc08f23	ote	TEXT	\N	2025-12-10 00:32:57.05771	f	t
92	48	a189e205-4a21-496b-948d-f610ffc08f23	bác ơi	TEXT	\N	2025-12-10 00:33:59.673435	f	f
93	48	a189e205-4a21-496b-948d-f610ffc08f23	chào	TEXT	\N	2025-12-10 00:38:33.317152	f	f
94	48	a189e205-4a21-496b-948d-f610ffc08f23	chàoooooooooo	TEXT	\N	2025-12-10 00:38:47.98373	f	f
95	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	ừ chào	TEXT	\N	2025-12-10 00:38:52.347351	f	t
96	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	chào cu	TEXT	\N	2025-12-10 00:39:20.022079	f	f
97	48	a189e205-4a21-496b-948d-f610ffc08f23	ờ	TEXT	\N	2025-12-10 00:39:29.550484	f	t
98	48	a189e205-4a21-496b-948d-f610ffc08f23	okkk	TEXT	\N	2025-12-10 00:39:35.311969	f	t
99	60	a189e205-4a21-496b-948d-f610ffc08f23	chào bác	TEXT	\N	2025-12-13 15:03:27.109072	f	f
100	60	db969247-58a1-447d-a247-88582eedbfcd	chào cậu	TEXT	\N	2025-12-13 15:03:35.847226	f	t
\.


--
-- TOC entry 5119 (class 0 OID 321224)
-- Dependencies: 242
-- Data for Name: chat_participant; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat_participant (id, room_id, user_id, role, joined_at) FROM stdin;
117	59	a189e205-4a21-496b-948d-f610ffc08f23	PATIENT	2025-12-10 12:05:46.132346
118	59	44ec66f7-8ab0-4e65-b47e-f11df325d938	DOCTOR	2025-12-10 12:05:46.136345
119	60	a189e205-4a21-496b-948d-f610ffc08f23	PATIENT	2025-12-13 15:03:04.785101
120	60	db969247-58a1-447d-a247-88582eedbfcd	DOCTOR	2025-12-13 15:03:04.793309
121	61	a189e205-4a21-496b-948d-f610ffc08f23	PATIENT	2025-12-16 17:01:30.837176
122	61	b0d87dbd-fe60-463a-b7ed-ed53770082a7	DOCTOR	2025-12-16 17:01:30.841246
95	48	a189e205-4a21-496b-948d-f610ffc08f23	PATIENT	2025-12-08 16:29:03.367098
96	48	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	DOCTOR	2025-12-08 16:29:03.370111
97	49	11eaee07-3b27-4821-80c8-46bfeaf4a0b1	PATIENT	2025-12-09 09:24:07.894632
98	49	b0d87dbd-fe60-463a-b7ed-ed53770082a7	DOCTOR	2025-12-09 09:24:07.899147
\.


--
-- TOC entry 5117 (class 0 OID 321216)
-- Dependencies: 240
-- Data for Name: chat_room; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat_room (id, type, created_at) FROM stdin;
59	ONE_TO_ONE	2025-12-10 12:05:46.085213
60	ONE_TO_ONE	2025-12-13 15:03:04.73358
61	ONE_TO_ONE	2025-12-16 17:01:30.797301
48	ONE_TO_ONE	2025-12-08 16:29:03.363096
49	ONE_TO_ONE	2025-12-09 09:24:07.861546
\.


--
-- TOC entry 5106 (class 0 OID 312438)
-- Dependencies: 229
-- Data for Name: diagnosis_templates; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.diagnosis_templates (id, template_name, diagnosis_content, "icd10Code", doctor_id, created_at, icd10code) FROM stdin;
450fb688-a978-47e0-878a-5e2cea1c1800	Đơn thuốc cảm cúm cơ bản	Chẩn đoán: Cảm cúm thông thường.	\N	44ec66f7-8ab0-4e65-b47e-f11df325d938	2025-10-11 18:58:04.371841+07	J11.1
\.


--
-- TOC entry 5113 (class 0 OID 312649)
-- Dependencies: 236
-- Data for Name: doctor_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_profiles (user_id, specialty, degree) FROM stdin;
db969247-58a1-447d-a247-88582eedbfcd	Nha khoa Tổng quát	Thạc sĩ Y học (ThS.BS)
b0d87dbd-fe60-463a-b7ed-ed53770082a7	Nha khoa Tổng quát	Tiến sĩ (TS)
11eaee07-3b27-4821-80c8-46bfeaf4a0b1	Y Khoa	Phó giáo sư (Psg)
44ec66f7-8ab0-4e65-b47e-f11df325d938	Phụ Khoa	Bác Sĩ (Bs)
\.


--
-- TOC entry 5100 (class 0 OID 303984)
-- Dependencies: 223
-- Data for Name: inventory; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.inventory (product_id, branch_id, quantity, expiry_date, last_updated_at) FROM stdin;
d17e1038-d806-491a-ab11-1fc6cb20d1d7	649847b6-3def-4a78-bbe1-f480b4bbbfaf	50	\N	2025-10-06 15:24:03.070677+07
bf7c7ee3-d0db-4353-b64b-3fa43c1639ff	649847b6-3def-4a78-bbe1-f480b4bbbfaf	63	\N	2025-10-24 17:24:46.208668+07
97f3dd1a-c32a-41a5-9fb6-23062a40ff67	649847b6-3def-4a78-bbe1-f480b4bbbfaf	8	\N	2025-11-21 17:16:30.576211+07
141dbe25-88a7-4219-a680-5cc2601a88cc	649847b6-3def-4a78-bbe1-f480b4bbbfaf	8	\N	2025-11-25 09:23:20.693669+07
4c0b79b9-9431-4ebb-8119-e33079e927f9	649847b6-3def-4a78-bbe1-f480b4bbbfaf	43	\N	2025-12-01 16:18:42.186536+07
6e7df233-4a39-4780-86c5-0e9d2cfaba98	649847b6-3def-4a78-bbe1-f480b4bbbfaf	48	\N	2025-12-10 11:55:51.369858+07
c133bb4a-fa68-43a6-8871-b12ae3e01128	649847b6-3def-4a78-bbe1-f480b4bbbfaf	9	\N	2025-12-16 17:04:50.19627+07
\.


--
-- TOC entry 5112 (class 0 OID 312621)
-- Dependencies: 235
-- Data for Name: medical_record_services; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.medical_record_services (medical_record_id, service_id) FROM stdin;
84edb7c8-37a0-4e9e-acc6-73dabcbf377c	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
84edb7c8-37a0-4e9e-acc6-73dabcbf377c	fffa8048-011a-4cf1-aa37-6194bba43ac1
572e33f8-e8ad-4088-9505-4c6709bae5f3	fffa8048-011a-4cf1-aa37-6194bba43ac1
7fd1a041-6966-41e5-9667-294d04f37bdf	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8
7fd1a041-6966-41e5-9667-294d04f37bdf	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
2d25d886-ab43-4677-bdd8-c5b221b3c236	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8
2d25d886-ab43-4677-bdd8-c5b221b3c236	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
9c47d3ab-51cd-47bf-8b34-d43ea2c317b5	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8
9c47d3ab-51cd-47bf-8b34-d43ea2c317b5	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
16f929a9-ee45-4b67-997b-b8f255368d7c	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8
16f929a9-ee45-4b67-997b-b8f255368d7c	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
ee9e7cb3-6481-4c4f-be06-173d012a9e2c	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8
ee9e7cb3-6481-4c4f-be06-173d012a9e2c	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
bf2dc4ac-b765-4c03-9f4e-ed2c75679d7c	1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8
bf2dc4ac-b765-4c03-9f4e-ed2c75679d7c	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
2aef29ca-6029-4be9-b5a6-299c3c94edb9	fffa8048-011a-4cf1-aa37-6194bba43ac1
59ee0e70-298d-49ed-bc89-a3338c7e1f2f	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
\.


--
-- TOC entry 5102 (class 0 OID 304032)
-- Dependencies: 225
-- Data for Name: medical_records; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.medical_records (id, appointment_id, diagnosis, e_signature, is_locked, created_at, updated_at, icd10code) FROM stdin;
eace647e-8c6f-4918-b302-c66f9bd64c0f	c363ef2d-04f8-4112-9f37-baa8e9477308	Viêm họng cấp	\N	f	2025-10-02 23:41:30.753876+07	2025-10-02 23:41:30.753876+07	J02.9
e2303ae6-f632-4560-945a-0b6148bbc32c	73f18a1d-6209-4c5b-a337-dfe370b64d66	Viêm họng cấp	\N	f	2025-10-02 23:44:32.832837+07	2025-10-02 23:44:32.832837+07	J02.9
1c6d9bea-b79a-4eea-a23c-a1e13dc11005	ef8ee174-71fb-4286-a50e-62416be3449d	Viêm họng cấp	\N	f	2025-10-03 00:02:56.44903+07	2025-10-03 00:02:56.44903+07	J02.9
6cdb2e2a-e930-4208-88b6-a812c25ad9d9	1834a319-f9bc-4a52-b811-f82619241f13	Viêm họng cấp	\N	f	2025-10-03 00:13:00.417241+07	2025-10-03 00:13:00.417241+07	J02.9
f6eba578-6750-4e89-b6b5-6dd9cd052813	ae9b61b7-b5cf-4b59-9595-3f31fd71c32a	Sốt	\N	f	2025-10-04 18:19:17.952419+07	2025-10-04 18:19:17.952419+07	J02.9
196db182-c70c-4313-a744-83f52d8822be	635d9680-9f0c-40c3-a359-7626cdb594a6	Sốt	\N	f	2025-10-06 14:39:58.098412+07	2025-10-06 14:39:58.098412+07	J02.9
c08c6095-8cf6-450e-8893-88be130d1887	3a486dde-c882-4873-861b-63220f3d5a29	Viêm họng cấp	Dr. Strange - Signed at 2025-10-08	t	2025-10-02 16:57:43.079282+07	2025-10-08 11:13:48.282728+07	J02.9
31862511-30d8-4b48-aebc-ffe1eba8165c	25720c73-6213-45a7-8a60-b1d99e4379b9	Chẩn đoán: Cảm cúm thông thường.	\N	f	2025-10-12 09:44:43.628343+07	2025-10-12 09:44:43.628343+07	J11.1
84edb7c8-37a0-4e9e-acc6-73dabcbf377c	a21b3bd6-8a59-4011-9163-50a3a5d5c2e5	Sốt	\N	f	2025-10-24 17:12:34.716774+07	2025-10-24 17:12:34.716774+07	J02.9
572e33f8-e8ad-4088-9505-4c6709bae5f3	46e9d64b-c150-495b-9d1b-fc6b34ccf52c	Viêm họng cấp	\N	f	2025-11-20 15:32:33.180838+07	2025-11-20 15:32:33.180838+07	
7fd1a041-6966-41e5-9667-294d04f37bdf	ff12dac9-bc25-4a48-ba33-cdaad43abb14	Viêm họng cấp	\N	f	2025-11-21 11:03:50.455738+07	2025-11-21 11:03:50.455738+07	
2d25d886-ab43-4677-bdd8-c5b221b3c236	424e67a8-380d-4ef2-8a97-fb92110c65eb	ốm do thay đổi thời tiết	\N	f	2025-11-21 14:26:46.169399+07	2025-11-21 14:26:46.169399+07	
9c47d3ab-51cd-47bf-8b34-d43ea2c317b5	a04a61fe-775c-49cb-abac-0ca2b8bf2867	ốm do thay đổi thời tiết	\N	f	2025-11-21 14:56:47.677306+07	2025-11-21 14:56:47.677306+07	
16f929a9-ee45-4b67-997b-b8f255368d7c	e45e9401-54e2-406e-96ba-9ae1d582b441	ốm do thay đổi thời tiết	\N	f	2025-11-24 20:36:18.412564+07	2025-11-24 20:36:18.412564+07	
ee9e7cb3-6481-4c4f-be06-173d012a9e2c	cc1cdd1b-05e6-481d-af25-7d53e92fe3fd	ốm em muôn	\N	f	2025-11-24 21:13:32.958682+07	2025-11-24 21:13:32.958682+07	JS112
bf2dc4ac-b765-4c03-9f4e-ed2c75679d7c	6f9b3d8c-7773-4670-b531-893701459d89	Viêm họng cấp	\N	f	2025-11-25 09:21:47.599912+07	2025-11-27 17:22:25.169518+07	
2aef29ca-6029-4be9-b5a6-299c3c94edb9	786a6785-982d-4bf3-a03b-f9cd3eb1fbbf	chết cmnr	\N	f	2025-12-10 11:01:21.600289+07	2025-12-10 11:01:21.600289+07	
59ee0e70-298d-49ed-bc89-a3338c7e1f2f	f7f251c7-27c6-4bf2-b4b4-646c8c8224d8	chết cmnr	\N	f	2025-12-16 17:04:03.546842+07	2025-12-16 17:04:03.546842+07	
\.


--
-- TOC entry 5097 (class 0 OID 303950)
-- Dependencies: 220
-- Data for Name: patient_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.patient_profiles (user_id, date_of_birth, gender, address, allergies, contraindications, medical_history, membership_tier, points) FROM stdin;
64ffbdb3-d785-4c50-a236-393b89f821e2	2000-05-09	male	Dallas	Dị ứng thuốc chống mọc tóc	Không có.	Không có.	STANDARD	0
613965fd-be3e-4ffa-aae8-e47ae84a3221	\N	\N	\N	\N	\N	\N	STANDARD	0
567bb94e-ec65-404a-8abd-4382b27b6141	\N	\N	\N	\N	\N	\N	STANDARD	0
a189e205-4a21-496b-948d-f610ffc08f23	\N	\N	\N	\N	\N	\N	STANDARD	50
\.


--
-- TOC entry 5111 (class 0 OID 312511)
-- Dependencies: 234
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payments (id, appointment_id, amount, payment_method, status, order_id, transaction_id, created_at, updated_at, bill_id, paid_at) FROM stdin;
7c4514b5-e55f-4c44-8cb3-4a2ac5bc601e	77033994-970f-4292-b259-2854d7849fe8	500000.00	VNPAY	PENDING	77033994-970f-4292-b259-2854d7849fe8	\N	2025-10-14 14:42:54.344475+07	2025-10-14 14:42:54.344475+07	\N	\N
7f5ecdb4-9f2e-4c82-8883-88d7b9b6dcbe	2d6e8320-5118-43bb-a377-99fda0660334	500000.00	VNPAY	PENDING	2d6e8320-5118-43bb-a377-99fda0660334	\N	2025-10-14 14:43:40.920529+07	2025-10-14 14:43:40.920529+07	\N	\N
04b80d36-6420-4ca8-81f1-f63ff6b0fbe3	7196d830-db59-40ed-a94d-053687f9b1aa	500000.00	VNPAY	PENDING	7196d830-db59-40ed-a94d-053687f9b1aa	\N	2025-10-14 14:46:33.146334+07	2025-10-14 14:46:33.146334+07	\N	\N
6d5d83d1-8ab0-44f2-9c6d-aed9ed65e706	94357ea5-5256-440c-8748-f5a8905fd4d0	500000.00	VNPAY	PENDING	94357ea5-5256-440c-8748-f5a8905fd4d0	\N	2025-10-14 14:58:02.690394+07	2025-10-14 14:58:02.690394+07	\N	\N
1d07b5ba-8944-417b-a32a-b7c13d037e2b	145138b9-3233-41a0-be13-3857be38bf2c	500000.00	VNPAY	PENDING	145138b9-3233-41a0-be13-3857be38bf2c	\N	2025-10-14 15:01:57.940506+07	2025-10-14 15:01:57.940506+07	\N	\N
ed9d6146-2dee-4048-a5dc-e762b8e75ffc	04f10759-5ed4-4678-a344-feaf2c5c64e5	500000.00	VNPAY	PENDING	04f10759-5ed4-4678-a344-feaf2c5c64e5	\N	2025-10-14 15:02:55.480939+07	2025-10-14 15:02:55.480939+07	\N	\N
b90ce3f4-3f6c-4afd-aa25-d0adb85d498c	e1a17517-b4f1-485e-a20b-7d72678f7e7c	500000.00	VNPAY	PENDING	e1a17517-b4f1-485e-a20b-7d72678f7e7c	\N	2025-10-14 15:05:27.959597+07	2025-10-14 15:05:27.959597+07	\N	\N
67d1c878-12ec-4f86-8c25-669890e9b7ff	d3a05bcd-755b-4c8e-acec-acf29cc88d29	500000.00	VNPAY	PENDING	d3a05bcd-755b-4c8e-acec-acf29cc88d29	\N	2025-10-14 15:10:55.378952+07	2025-10-14 15:10:55.378952+07	\N	\N
2845d041-4eec-437a-8ed8-66a18ef68111	9a3a4bbd-5a68-4147-8eb2-c1e0f8ed7bea	500000.00	VNPAY	PENDING	9a3a4bbd-5a68-4147-8eb2-c1e0f8ed7bea	\N	2025-10-14 15:16:10.913743+07	2025-10-14 15:16:10.913743+07	\N	\N
6a37f464-6316-40ea-869d-f60559537fc0	312c96fd-411f-4196-a7a2-d77b0bbe01e5	500000.00	VNPAY	PENDING	312c96fd-411f-4196-a7a2-d77b0bbe01e5	\N	2025-10-14 15:17:12.400611+07	2025-10-14 15:17:12.400611+07	\N	\N
97c2d19c-c71f-4c7b-a3e6-c462532041dc	7d01e4eb-5d7f-4418-803a-73d4b59ad354	500000.00	VNPAY	PENDING	7d01e4eb-5d7f-4418-803a-73d4b59ad354	\N	2025-10-14 15:19:46.783777+07	2025-10-14 15:19:46.783777+07	\N	\N
4f65392f-875a-4e6f-8ba7-1dd042ef4f62	853acfca-6981-4d10-b7f0-d7041b76a7b3	500000.00	VNPAY	PENDING	853acfca-6981-4d10-b7f0-d7041b76a7b3	\N	2025-10-14 15:35:38.379562+07	2025-10-14 15:35:38.379562+07	\N	\N
da4adc1b-3045-4a77-a0ed-09250bb285cd	5722fd8c-2ba2-4594-ae7d-3cb0eba94210	500000.00	VNPAY	PENDING	5722fd8c-2ba2-4594-ae7d-3cb0eba94210	\N	2025-10-14 15:37:55.15848+07	2025-10-14 15:37:55.15848+07	\N	\N
643b3617-ec0e-4343-ac57-70206d118d23	5a500b49-f1c1-4598-9479-5f655bb376d6	500000.00	VNPAY	PENDING	5a500b49-f1c1-4598-9479-5f655bb376d6	\N	2025-10-14 15:43:48.863466+07	2025-10-14 15:43:48.863466+07	\N	\N
77190847-2575-47a2-9f7f-f2fbedbff36f	b4573eec-823f-4784-b5bb-ad6cf49c6649	500000.00	VNPAY	PENDING	b4573eec-823f-4784-b5bb-ad6cf49c6649	\N	2025-10-14 16:05:54.997454+07	2025-10-14 16:05:54.997454+07	\N	\N
46c41758-98ea-4267-b2be-0d808eaebf8c	c4121f6a-959a-47ea-b609-8e66dd61e3e2	500000.00	VNPAY	PENDING	c4121f6a-959a-47ea-b609-8e66dd61e3e2	\N	2025-10-14 16:11:52.981475+07	2025-10-14 16:11:52.981475+07	\N	\N
99584e99-48ef-4018-941f-4cedf44faae0	e978e91d-dd53-4596-91a1-418b0d5591e7	500000.00	VNPAY	PENDING	e978e91d-dd53-4596-91a1-418b0d5591e7	\N	2025-10-14 16:15:09.744715+07	2025-10-14 16:15:09.744715+07	\N	\N
1bade80e-9156-4b16-b1f6-63393e947196	eec99efa-04fc-4894-8f6c-c60a1ea6ca83	500000.00	VNPAY	PENDING	eec99efa-04fc-4894-8f6c-c60a1ea6ca83	\N	2025-10-14 16:26:33.644248+07	2025-10-14 16:26:33.644248+07	\N	\N
f75fe1c2-4c06-49e4-ba8f-13fe12daee02	976b94c2-03ad-43eb-b22b-176f4279a12b	500000.00	VNPAY	PENDING	976b94c2-03ad-43eb-b22b-176f4279a12b	\N	2025-10-14 17:07:35.581984+07	2025-10-14 17:07:35.581984+07	\N	\N
ade9a252-0ef7-44b3-990d-c9a7f4ea51a1	a9c1d4cf-6f5e-448c-950f-63518b9b7ca6	500000.00	VNPAY	PENDING	a9c1d4cf-6f5e-448c-950f-63518b9b7ca6	\N	2025-10-14 17:09:19.040564+07	2025-10-14 17:09:19.040564+07	\N	\N
4b07a91e-6c68-4ad7-8525-44ae00f94e3d	01f40ef5-7ecf-47c3-a845-d10335f5b2de	500000.00	VNPAY	PENDING	01f40ef5-7ecf-47c3-a845-d10335f5b2de	\N	2025-10-15 10:02:52.06483+07	2025-10-15 10:02:52.06483+07	\N	\N
eb710eb8-c0f0-463e-80a0-ebe915ecb5cc	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	d66d44a0-3a4e-452b-beda-8236b215f7b3	\N	2025-10-15 10:54:27.459577+07	2025-10-15 10:54:27.459577+07	\N	\N
b9014b68-d0c5-4cd6-a597-ae09aa5e38d6	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	d66d44a0-3a4e-452b-beda-8236b215f7b3	\N	2025-10-15 11:07:41.547541+07	2025-10-15 11:07:41.547541+07	\N	\N
2e9c893d-4a4e-45fa-a878-b26352c6cdad	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	d66d44a0-3a4e-452b-beda-8236b215f7b3	\N	2025-10-15 11:09:58.504825+07	2025-10-15 11:09:58.504825+07	\N	\N
135e54bf-1396-44b8-9f41-0e61893cd1d5	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	d66d44a0-3a4e-452b-beda-8236b215f7b3	\N	2025-10-15 11:13:18.819527+07	2025-10-15 11:13:18.819527+07	\N	\N
ce9a3e36-5537-4c5f-b15c-f3c21ad9f71c	b4573eec-823f-4784-b5bb-ad6cf49c6649	500000.00	VNPAY	PENDING	b4573eec-823f-4784-b5bb-ad6cf49c6649	\N	2025-10-15 11:13:31.725948+07	2025-10-15 11:13:31.725948+07	\N	\N
c3c8f815-091e-43c9-9f81-457f1a87b4a4	5722fd8c-2ba2-4594-ae7d-3cb0eba94210	500000.00	VNPAY	PENDING	5722fd8c-2ba2-4594-ae7d-3cb0eba94210	\N	2025-10-15 11:21:57.356386+07	2025-10-15 11:21:57.356386+07	\N	\N
d4660822-b476-4a9d-ae80-7093aec5424a	01f40ef5-7ecf-47c3-a845-d10335f5b2de	500000.00	VNPAY	PENDING	01f40ef5-7ecf-47c3-a845-d10335f5b2de	\N	2025-10-15 11:27:36.085332+07	2025-10-15 11:27:36.085332+07	\N	\N
ace5b7ba-3e8c-4177-a16b-b555c2d02887	a9c1d4cf-6f5e-448c-950f-63518b9b7ca6	500000.00	VNPAY	PENDING	a9c1d4cf-6f5e-448c-950f-63518b9b7ca6	\N	2025-10-15 11:27:48.543093+07	2025-10-15 11:27:48.543093+07	\N	\N
9f47f85e-044b-41be-9346-8f252e4c84d4	853acfca-6981-4d10-b7f0-d7041b76a7b3	500000.00	VNPAY	PENDING	853acfca-6981-4d10-b7f0-d7041b76a7b3	\N	2025-10-15 11:31:24.817088+07	2025-10-15 11:31:24.817088+07	\N	\N
b09c33c8-2e5d-494d-aabb-e54c82f61a15	c4121f6a-959a-47ea-b609-8e66dd61e3e2	500000.00	VNPAY	PENDING	c4121f6a-959a-47ea-b609-8e66dd61e3e2	\N	2025-10-15 11:40:43.64558+07	2025-10-15 11:40:43.64558+07	\N	\N
036a4c9e-25c0-4474-b0ee-cc73003c1945	d3a05bcd-755b-4c8e-acec-acf29cc88d29	500000.00	VNPAY	PENDING	d3a05bcd-755b-4c8e-acec-acf29cc88d29	\N	2025-10-15 11:54:12.470676+07	2025-10-15 11:54:12.470676+07	\N	\N
0509866f-2e27-4c92-b949-34a5af9a5740	e1a17517-b4f1-485e-a20b-7d72678f7e7c	500000.00	VNPAY	PENDING	e1a17517-b4f1-485e-a20b-7d72678f7e7c	\N	2025-10-15 11:59:38.017108+07	2025-10-15 11:59:38.017108+07	\N	\N
9b5f275e-88f5-4995-a8c3-e7db3fc99965	c4121f6a-959a-47ea-b609-8e66dd61e3e2	500000.00	VNPAY	PENDING	c4121f6a-959a-47ea-b609-8e66dd61e3e2	\N	2025-10-15 22:35:29.476347+07	2025-10-15 22:35:29.476347+07	\N	\N
dbbea334-bed0-4e77-8945-09f55689d9ef	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	d66d44a0-3a4e-452b-beda-8236b215f7b3	\N	2025-10-15 23:01:18.593433+07	2025-10-15 23:01:18.593433+07	\N	\N
028c4524-8cd3-44cc-8f72-43d9cd6d234b	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	d66d44a0-3a4e-452b-beda-8236b215f7b3	\N	2025-10-15 11:12:59.742888+07	2025-10-15 11:12:59.742888+07	\N	\N
907842d2-310b-4ceb-a5b6-e4e7ae1bb30e	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	8828fd644f0d	\N	2025-10-15 23:19:39.691409+07	2025-10-15 23:19:39.692418+07	\N	\N
c0be3a81-fcee-4c0d-9b9d-a2f548ecb9e8	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	d8cf4df670ba	\N	2025-10-15 23:22:57.866149+07	2025-10-15 23:22:57.866149+07	\N	\N
8cab5e23-8071-4578-a548-89e7f17b6c2f	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	e32759c8a50d	\N	2025-10-15 23:26:45.698453+07	2025-10-15 23:26:45.698453+07	\N	\N
1699643f-121c-4dc5-8554-c05d63adad35	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	1adb2e18465f	\N	2025-10-15 23:29:33.67981+07	2025-10-15 23:29:33.67981+07	\N	\N
9e78dc73-3546-40bf-9c47-00d024fedab3	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	c0ba15740b18	\N	2025-10-15 23:31:41.422402+07	2025-10-15 23:31:41.422402+07	\N	\N
7597bc0c-475e-4d3d-b9fc-bac7f7e2e7ae	d66d44a0-3a4e-452b-beda-8236b215f7b3	500000.00	VNPAY	PENDING	ORD1760602573300	\N	2025-10-16 15:16:13.319215+07	2025-10-16 15:16:13.319215+07	\N	\N
410058dc-ca79-4947-bb1e-38301f5935b2	e1fc60b8-1999-4c4e-823b-54776f79c723	500000.00	VNPAY	PENDING	ORD1760602874090	\N	2025-10-16 15:21:14.106214+07	2025-10-16 15:21:14.106214+07	\N	\N
8e684032-8075-4fc1-93b3-cf393e298da9	46e9d64b-c150-495b-9d1b-fc6b34ccf52c	500000.00	VNPAY	PAID	ORD1760603435193	12345678	2025-10-16 15:30:35.197388+07	2025-10-16 16:10:26.533538+07	\N	\N
adf2a29e-c3c3-4ec0-b80f-35810630d0b3	43c59d44-4648-452e-ac9a-acdccd44d57a	500000.00	VNPAY	PENDING	ORD1760607190550	\N	2025-10-16 16:33:10.554438+07	2025-10-16 16:33:10.554438+07	\N	\N
365d6445-0025-4348-a6d9-eed5bff19e76	43c59d44-4648-452e-ac9a-acdccd44d57a	500000.00	VNPAY	PENDING	ORD1760607422797	\N	2025-10-16 16:37:02.813112+07	2025-10-16 16:37:02.813112+07	\N	\N
a8ccd4e4-d87c-4223-b958-37cb9376c3c6	55238102-01ee-4fb5-bc76-1c5d589728ff	500000.00	VNPAY	PENDING	ORD1760607655340	\N	2025-10-16 16:40:55.342772+07	2025-10-16 16:40:55.342772+07	\N	\N
4f4fe342-034c-4e56-aecc-97ac9244b029	55238102-01ee-4fb5-bc76-1c5d589728ff	500000.00	VNPAY	PENDING	ORD1760607982349	\N	2025-10-16 16:46:22.36617+07	2025-10-16 16:46:22.367184+07	\N	\N
9300188f-4501-43f7-981b-f53950ff3964	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760608075087	\N	2025-10-16 16:47:55.088546+07	2025-10-16 16:47:55.088546+07	\N	\N
9bf1c197-6bdb-4e4d-8c7f-69e50b8984c6	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760608507389	\N	2025-10-16 16:55:07.404842+07	2025-10-16 16:55:07.404842+07	\N	\N
23edb7cf-c9ce-44d4-8a6f-538a04af746f	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760608759728	\N	2025-10-16 16:59:19.746077+07	2025-10-16 16:59:19.746077+07	\N	\N
65bcbc43-d4e2-4d45-b486-297692e94659	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760608860812	\N	2025-10-16 17:01:00.828808+07	2025-10-16 17:01:00.828808+07	\N	\N
acf0483e-5dba-46bd-a444-5bd93fa9aa7a	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760609249003	\N	2025-10-16 17:07:29.02117+07	2025-10-16 17:07:29.02117+07	\N	\N
53cdeadb-d955-46d6-9eb0-e920be81b811	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760682815981	\N	2025-10-17 13:33:36.004486+07	2025-10-17 13:33:36.004486+07	\N	\N
80311614-8a83-4cf6-b4c1-576ec68327c0	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760682951120	\N	2025-10-17 13:35:51.138822+07	2025-10-17 13:35:51.138822+07	\N	\N
7dbcde29-77d4-403a-afb0-34bc4c2615c4	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760684061874	\N	2025-10-17 13:54:21.900699+07	2025-10-17 13:54:21.900699+07	\N	\N
a543dd84-b896-4d42-b487-22f671d663be	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760684067755	\N	2025-10-17 13:54:27.757402+07	2025-10-17 13:54:27.757402+07	\N	\N
1d003eee-5e56-41e2-abbc-a315957c14a0	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760684789819	\N	2025-10-17 14:06:29.837148+07	2025-10-17 14:06:29.839188+07	\N	\N
4fa5ea4b-6afe-4b03-84c9-f2049f8d7800	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760684871873	\N	2025-10-17 14:07:51.894063+07	2025-10-17 14:07:51.894063+07	\N	\N
fd2764b0-c677-44e6-824d-40e5f08aaeee	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760687001581	\N	2025-10-17 14:43:21.60261+07	2025-10-17 14:43:21.60261+07	\N	\N
b5f7991a-e84a-4ddc-9cf0-d85dca535c3c	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PENDING	ORD1760688494085	\N	2025-10-17 15:08:14.104278+07	2025-10-17 15:08:14.104278+07	\N	\N
13ee1f56-4094-4f1d-98f8-e7bb7e435ad9	99eb63fa-d8f4-452d-a018-ca83e5513c6b	500000.00	VNPAY	PAID	ORD1760692514828	15208003	2025-10-17 16:15:14.844073+07	2025-10-17 16:16:51.598252+07	\N	\N
aa207fa7-149b-42ba-844e-926cb0a2df09	a374505e-9eb3-4bc2-a134-72024f7e0fc4	500000.00	VNPAY	PENDING	ORD1760762315636	\N	2025-10-18 11:38:35.64054+07	2025-10-18 11:38:35.64054+07	\N	\N
5282db93-5c66-4f2d-b02e-c73ab519e1dc	a0c204f9-815c-4c2e-8378-50d7a961968e	500000.00	VNPAY	PENDING	ORD1760796462971	\N	2025-10-18 21:07:42.974618+07	2025-10-18 21:07:42.974618+07	\N	\N
c989993d-b0b1-4f21-b813-9d06d35a20fb	92cecd79-2ba1-4b13-8b32-3e880bfdb77f	500000.00	VNPAY	PENDING	ORD1760797367251	\N	2025-10-18 21:22:47.255695+07	2025-10-18 21:22:47.255695+07	\N	\N
0c51cd38-589b-43a1-8f97-18e64dd33dd0	92cecd79-2ba1-4b13-8b32-3e880bfdb77f	500000.00	VNPAY	PENDING	ORD1760797558310	\N	2025-10-18 21:25:58.330842+07	2025-10-18 21:25:58.330842+07	\N	\N
d59d0411-e186-4510-b5ec-8d2490b7dcf4	92cecd79-2ba1-4b13-8b32-3e880bfdb77f	500000.00	VNPAY	PENDING	ORD1760797559160	\N	2025-10-18 21:25:59.160866+07	2025-10-18 21:25:59.160866+07	\N	\N
91a3234d-5340-4926-90f7-49dc9d6f818e	92cecd79-2ba1-4b13-8b32-3e880bfdb77f	500000.00	VNPAY	PENDING	ORD1760797760416	\N	2025-10-18 21:29:20.432983+07	2025-10-18 21:29:20.432983+07	\N	\N
4695fe5b-edb0-498c-8f3b-69e02a68b786	92cecd79-2ba1-4b13-8b32-3e880bfdb77f	500000.00	VNPAY	PENDING	ORD1760798701083	\N	2025-10-18 21:45:01.114525+07	2025-10-18 21:45:01.114525+07	\N	\N
1a8fdce1-41ce-4d6a-9dbc-0dd937108f83	92cecd79-2ba1-4b13-8b32-3e880bfdb77f	500000.00	VNPAY	PAID	ORD1760798892725	15209220	2025-10-18 21:48:12.741784+07	2025-10-18 21:55:56.169412+07	\N	\N
a1e3a2b1-77cb-4286-bcd9-68bb14c0b8fb	53550399-6c48-4a93-93ee-7ffb83f0227e	500000.00	VNPAY	PAID	ORD1760799411175	15209224	2025-10-18 21:56:51.177667+07	2025-10-18 21:57:23.303559+07	\N	\N
7a7c4084-be38-4adc-8d9f-65670cfc57c3	a21b3bd6-8a59-4011-9163-50a3a5d5c2e5	150000.00	VNPAY	PAID	ORD1761121715267	15214152	2025-10-22 15:28:35.273193+07	2025-10-22 15:32:35.289525+07	\N	\N
62e5a9fb-4e55-4398-ad08-c61ef92bf74a	a21b3bd6-8a59-4011-9163-50a3a5d5c2e5	910000.00	VNPAY	PAID	84edb7c8-37a0-4e9e-acc6-73dabcbf377c	15217519	2025-10-24 17:22:56.575482+07	2025-10-24 17:24:46.271115+07	\N	\N
7dce8274-addf-4362-b34b-319a324e4d89	599a8ac1-cd52-48dc-b5c6-a46b9234fb20	150000.00	VNPAY	PAID	ORD599a8ac1-cd52-48dc-b5c6-a46b9234fb20	15229757	2025-10-31 14:32:12.973329+07	2025-10-31 14:34:05.433945+07	\N	\N
849dbf3e-de67-4f1d-bdec-4af279b52c37	46e9d64b-c150-495b-9d1b-fc6b34ccf52c	550000.00	VNPAY	PENDING	MR-572e33f8-e8ad-4088-9505-4c6709bae5f3	\N	2025-11-21 00:37:22.622569+07	2025-11-21 00:37:22.622569+07	\N	\N
d2d217e6-c60a-40f6-8bce-a92ad99fb9a3	46e9d64b-c150-495b-9d1b-fc6b34ccf52c	550000.00	VNPAY	PENDING	MR-572e33f8-e8ad-4088-9505-4c6709bae5f3	\N	2025-11-21 00:40:45.873648+07	2025-11-21 00:40:45.873648+07	\N	\N
faafcf8f-6fed-4a61-80ea-2995b59d84c6	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 00:46:11.746913+07	2025-11-21 00:46:11.746913+07	\N	\N
01a796c6-9b10-499e-ba48-ab9f406fd272	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 00:54:35.280825+07	2025-11-21 00:54:35.280825+07	\N	\N
2025fd80-aeb6-4180-84be-764dc6c92319	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 00:56:51.08699+07	2025-11-21 00:56:51.08699+07	\N	\N
0aab97aa-d922-4ad4-905d-cbbf5d5e75f6	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 09:31:07.429215+07	2025-11-21 09:31:07.430215+07	\N	\N
362144d0-f3b9-4898-96e2-c798c3bfab90	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 09:47:23.154545+07	2025-11-21 09:47:23.154545+07	\N	\N
868ed759-d185-4677-ac1f-7869bf4a2fb7	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 10:01:27.19491+07	2025-11-21 10:01:27.19491+07	\N	\N
890af00f-24b9-4a18-a36b-a8c68e944014	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 10:03:27.442278+07	2025-11-21 10:03:27.442278+07	\N	\N
04ad3f0e-ca33-4e18-ac71-cbd7b57af2d3	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 10:10:00.606462+07	2025-11-21 10:10:00.606462+07	\N	\N
7e60d57d-b732-4001-b544-4508cadc907b	f205b6e6-48da-4a34-9ccd-b50bc1865795	150000.00	VNPAY	PENDING	ORDf205b6e6-48da-4a34-9ccd-b50bc1865795	\N	2025-11-21 10:28:49.229989+07	2025-11-21 10:28:49.229989+07	\N	\N
f64c3c0f-6733-476e-bf31-ef671cc73ad7	9288ef73-4b7a-485c-9213-a188cc891836	150000.00	VNPAY	PENDING	ORD9288ef73-4b7a-485c-9213-a188cc891836	\N	2025-11-21 10:37:55.482641+07	2025-11-21 10:37:55.482641+07	\N	\N
b1819dff-cda1-4a40-9546-d8c5e411081e	9288ef73-4b7a-485c-9213-a188cc891836	150000.00	VNPAY	PENDING	ORD9288ef73-4b7a-485c-9213-a188cc891836	\N	2025-11-21 10:39:07.844912+07	2025-11-21 10:39:07.844912+07	\N	\N
4dba4f25-ab42-4dfb-8c15-2d49e18c02b1	ff12dac9-bc25-4a48-ba33-cdaad43abb14	150000.00	VNPAY	PAID	ORDff12dac9-bc25-4a48-ba33-cdaad43abb14	15274935	2025-11-21 10:40:58.764762+07	2025-11-21 10:42:04.533876+07	\N	\N
271070f2-7fb8-4d28-8657-f30f46e88c8c	bea11f55-65cc-4a16-a145-3eef3025c094	150000.00	VNPAY	PAID	ORDbea11f55-65cc-4a16-a145-3eef3025c094	15274941	2025-11-21 10:44:28.818242+07	2025-11-21 10:45:15.274161+07	\N	\N
5e18ce26-3de7-4093-9244-54cf0f7c92ef	ff12dac9-bc25-4a48-ba33-cdaad43abb14	750000.00	VNPAY	PAID	MR-7fd1a041-6966-41e5-9667-294d04f37bdf	15274995	2025-11-21 11:05:17.016775+07	2025-11-21 11:08:25.880872+07	\N	\N
03ad3aa1-bf4b-415a-b687-30302f4f5f1c	424e67a8-380d-4ef2-8a97-fb92110c65eb	150000.00	VNPAY	PAID	ORD424e67a8-380d-4ef2-8a97-fb92110c65eb	15276333	2025-11-21 14:24:23.91156+07	2025-11-21 14:25:24.49593+07	f3de7544-d218-4cbe-b34d-15cfb809be07	2025-11-21 14:25:10.354877+07
b00cc32d-e507-4e49-a59f-85730947f0fd	424e67a8-380d-4ef2-8a97-fb92110c65eb	750000.00	VNPAY	PAID	MR-2d25d886-ab43-4677-bdd8-c5b221b3c236	15276356	2025-11-21 14:29:31.300701+07	2025-11-21 14:31:16.962632+07	ad7c3db2-78c0-4b98-bd8f-dc5d624ff2c5	2025-11-21 14:31:03.370704+07
7e65759c-2c3c-4900-a26a-36b04f6ab681	30dd6b1e-e567-4f4e-a850-2b5e851dfe15	150000.00	VNPAY	PAID	ORD30dd6b1e-e567-4f4e-a850-2b5e851dfe15	15276410	2025-11-21 14:45:30.675148+07	2025-11-21 14:46:29.957312+07	10497358-6d3c-4191-bc0c-601e2fd8d5f1	2025-11-21 14:46:16.481809+07
60c2e0ba-46cc-4f2e-af6e-93d5b4a9e5a6	3acbdf35-bef7-493c-89e5-1d5db9af6bb5	150000.00	VNPAY	PAID	ORD3acbdf35-bef7-493c-89e5-1d5db9af6bb5	15276423	2025-11-21 14:50:06.280942+07	2025-11-21 14:51:01.734149+07	\N	2025-11-21 14:50:42.963884+07
f7cda9ab-5bc5-44ac-8fc4-80b28d38e3ae	a04a61fe-775c-49cb-abac-0ca2b8bf2867	150000.00	VNPAY	PAID	ORDa04a61fe-775c-49cb-abac-0ca2b8bf2867	15276435	2025-11-21 14:52:35.195579+07	2025-11-21 14:53:29.835466+07	8afc9990-60ef-4425-ad47-9a1ac9cc1f36	2025-11-21 14:53:16.103252+07
2df1347b-dbd7-46bf-bf05-f8747051baae	a04a61fe-775c-49cb-abac-0ca2b8bf2867	750000.00	VNPAY	PAID	MR-9c47d3ab-51cd-47bf-8b34-d43ea2c317b5	15276446	2025-11-21 14:56:53.054587+07	2025-11-21 14:57:39.14527+07	07a954f7-036e-4707-bb7f-26cae2a305cb	2025-11-21 14:57:25.495364+07
c99e3eee-2bd9-4962-8808-8b52c9b61b92	\N	80000.00	VNPAY	PAID	BILL-b7e6c4db-b1c8-4fbb-b25f-24cfb4932361	15276782	2025-11-21 16:25:48.554806+07	2025-11-21 16:27:24.337773+07	b7e6c4db-b1c8-4fbb-b25f-24cfb4932361	2025-11-21 16:27:24.255165+07
06893bae-6d64-4582-9d41-47e193e1dba3	\N	80000.00	VNPAY	PAID	BILL-8fdd2898-f54f-4668-a72d-6e5cacf847f9	15277749	2025-11-21 17:15:45.8711+07	2025-11-21 17:16:30.579213+07	8fdd2898-f54f-4668-a72d-6e5cacf847f9	2025-11-21 17:16:30.522673+07
72984039-fcc5-4e16-b77e-b7a551326f34	e45e9401-54e2-406e-96ba-9ae1d582b441	150000.00	VNPAY	PAID	ORDe45e9401-54e2-406e-96ba-9ae1d582b441	15284941	2025-11-24 20:34:05.125727+07	2025-11-24 20:35:40.41984+07	50bb6c07-0cfc-4021-8d31-7241f32ad6f7	2025-11-24 20:35:26.132756+07
d00357c1-ddfd-449c-ba2b-7ce3f6b6ed04	cc1cdd1b-05e6-481d-af25-7d53e92fe3fd	150000.00	VNPAY	PAID	ORDcc1cdd1b-05e6-481d-af25-7d53e92fe3fd	15284995	2025-11-24 21:11:51.941166+07	2025-11-24 21:12:39.163773+07	17eaa0b3-9913-433a-9f92-6e0397a12051	2025-11-24 21:12:25.154982+07
1f137398-b685-45e0-b685-f13d18626144	cc1cdd1b-05e6-481d-af25-7d53e92fe3fd	750000.00	VNPAY	PAID	MR-ee9e7cb3-6481-4c4f-be06-173d012a9e2c	15285025	2025-11-24 21:33:17.900438+07	2025-11-24 21:34:20.629601+07	f7e4abae-b2c7-4e62-9147-63c895f3d7fc	2025-11-24 21:34:06.245215+07
27315f19-bc4b-4c87-a61f-5d36e51df823	6f9b3d8c-7773-4670-b531-893701459d89	150000.00	VNPAY	PAID	ORD6f9b3d8c-7773-4670-b531-893701459d89	15285620	2025-11-25 09:19:53.454444+07	2025-11-25 09:21:05.687524+07	8773c6ea-e721-4d8c-b441-6c999b855281	2025-11-25 09:20:50.921863+07
f124f3b7-3d05-4286-9ba0-d3016045311c	6f9b3d8c-7773-4670-b531-893701459d89	750000.00	VNPAY	PAID	MR-bf2dc4ac-b765-4c03-9f4e-ed2c75679d7c	15285623	2025-11-25 09:22:39.956601+07	2025-11-25 09:23:34.751019+07	077f0d41-d81d-4353-aa84-31adacdcc7f1	2025-11-25 09:23:20.568771+07
f9860b37-d7e5-46e6-acf6-cd870a209670	c2f12c38-7bf3-4750-bbd8-26a888d39ee4	150000.00	VNPAY	PENDING	ORDc2f12c38-7bf3-4750-bbd8-26a888d39ee4	\N	2025-11-25 09:50:56.112979+07	2025-11-25 09:50:56.112979+07	\N	\N
14790b37-75b0-45e6-a257-49b54647b67f	\N	170000.00	VNPAY	PENDING	BILL-9f99f17f-aabb-4c9c-9dec-4f441464493e	\N	2025-11-26 14:33:22.812476+07	2025-11-26 14:33:22.812476+07	9f99f17f-aabb-4c9c-9dec-4f441464493e	\N
ed5767fc-0b39-4e50-9c46-3ee45226a5d7	\N	170000.00	VNPAY	PENDING	BILL-730ea8b4-8697-4b1d-91fa-3fc03a15a60c	\N	2025-11-26 14:34:43.109631+07	2025-11-26 14:34:43.109631+07	730ea8b4-8697-4b1d-91fa-3fc03a15a60c	\N
abd3e3dc-d785-40ae-b323-e40bf6e7bb4e	48b5a5c8-1d3a-4c78-8f31-c043845dd1a9	150000.00	VNPAY	PAID	ORD48b5a5c8-1d3a-4c78-8f31-c043845dd1a9	15327624	2025-12-08 17:01:44.423854+07	2025-12-08 17:04:29.590866+07	790fde3a-721c-49bf-a6ce-2b5ab3c6a5a1	2025-12-08 17:04:00.920563+07
e1ab5ed9-f576-458d-b5d3-22611a20c727	786a6785-982d-4bf3-a03b-f9cd3eb1fbbf	150000.00	VNPAY	PAID	ORD786a6785-982d-4bf3-a03b-f9cd3eb1fbbf	15331522	2025-12-10 10:57:04.827477+07	2025-12-10 10:57:56.383214+07	027062ea-0686-42f6-a27f-eeb684a6b2f8	2025-12-10 10:57:42.500037+07
b712a60d-cf76-4ac2-925c-e55a88200cfc	786a6785-982d-4bf3-a03b-f9cd3eb1fbbf	550000.00	VNPAY	PAID	MR-2aef29ca-6029-4be9-b5a6-299c3c94edb9	15331716	2025-12-10 11:55:26.929313+07	2025-12-10 11:56:04.656329+07	7bd232a8-297c-4d2d-8718-86c97f3ef155	2025-12-10 11:55:51.096343+07
259700a2-cd3f-4ace-aca0-41d464a208e5	79132aa7-9772-42a1-aebf-1876a05f3c4f	150000.00	VNPAY	PAID	ORD79132aa7-9772-42a1-aebf-1876a05f3c4f	15331737	2025-12-10 12:05:02.740771+07	2025-12-10 12:05:37.059175+07	65ef71e0-fdfa-4766-9fc7-ee7de90b00df	2025-12-10 12:05:23.824387+07
2bcc26f3-68dd-4311-945a-cf6ca052ec28	c2f12c38-7bf3-4750-bbd8-26a888d39ee4	150000.00	VNPAY	PENDING	ORDc2f12c38-7bf3-4750-bbd8-26a888d39ee4	\N	2025-12-10 12:32:43.646253+07	2025-12-10 12:32:43.646253+07	\N	\N
64d61047-3034-4c93-b6e3-7d160be1d97e	0b54e460-5526-4078-b752-32fa91ba77a0	150000.00	VNPAY	PENDING	ORD0b54e460-5526-4078-b752-32fa91ba77a0	\N	2025-12-14 16:04:08.591537+07	2025-12-14 16:04:08.591537+07	\N	\N
24690cec-3f45-47b8-824e-6da3c388ef7f	5e2d8854-815e-43e0-8db6-94142c147ca6	150000.00	VNPAY	PENDING	ORD5e2d8854-815e-43e0-8db6-94142c147ca6	\N	2025-12-14 16:04:35.901343+07	2025-12-14 16:04:35.901343+07	\N	\N
5d92da51-f018-464f-b4b5-172c22b7612f	f7f251c7-27c6-4bf2-b4b4-646c8c8224d8	150000.00	VNPAY	PAID	ORDf7f251c7-27c6-4bf2-b4b4-646c8c8224d8	15347955	2025-12-16 17:01:43.255952+07	2025-12-16 17:02:33.100017+07	08129198-60b1-482a-844d-cc8066d28368	2025-12-16 17:02:19.693317+07
413bff66-a673-4732-9ffd-24aefd6c2a8a	f7f251c7-27c6-4bf2-b4b4-646c8c8224d8	250000.00	VNPAY	PAID	MR-59ee0e70-298d-49ed-bc89-a3338c7e1f2f	15347963	2025-12-16 17:04:31.338505+07	2025-12-16 17:05:02.955267+07	a05c91ad-330e-4928-aa38-5dbad00cc933	2025-12-16 17:04:50.060365+07
\.


--
-- TOC entry 5105 (class 0 OID 304187)
-- Dependencies: 228
-- Data for Name: prescription_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prescription_items (id, medical_record_id, product_id, quantity, dosage, notes, created_at) FROM stdin;
1fef0bcb-7e50-447b-9714-c6468f9a8206	eace647e-8c6f-4918-b302-c66f9bd64c0f	4c0b79b9-9431-4ebb-8119-e33079e927f9	20	Uống 1 viên khi sốt trên 38.5 độ C	\N	2025-10-02 23:41:30.388957+07
91a962bd-aebc-4be8-bebf-b30c62bd4dca	eace647e-8c6f-4918-b302-c66f9bd64c0f	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	14	Uống 1 viên/lần, 2 lần/ngày sau ăn, dùng trong 7 ngày	\N	2025-10-02 23:41:30.388957+07
15d686ef-cb84-494f-99ff-3dd4f9f36056	e2303ae6-f632-4560-945a-0b6148bbc32c	4c0b79b9-9431-4ebb-8119-e33079e927f9	20	Uống 1 viên khi sốt trên 38.5 độ C	\N	2025-10-02 23:44:32.819373+07
5430a313-652b-4579-810e-bb3ed2095119	e2303ae6-f632-4560-945a-0b6148bbc32c	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	14	Uống 1 viên/lần, 2 lần/ngày sau ăn, dùng trong 7 ngày	\N	2025-10-02 23:44:32.819373+07
2b03cc85-4fe5-4b57-a99c-2dbd65297b5c	1c6d9bea-b79a-4eea-a23c-a1e13dc11005	4c0b79b9-9431-4ebb-8119-e33079e927f9	20	Uống 1 viên khi sốt trên 38.5 độ C	\N	2025-10-03 00:02:56.433159+07
9809bb3b-8dc3-46ed-97ce-8334a3fe4076	1c6d9bea-b79a-4eea-a23c-a1e13dc11005	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	14	Uống 1 viên/lần, 2 lần/ngày sau ăn, dùng trong 7 ngày	\N	2025-10-03 00:02:56.433159+07
6c75d9f6-ecc6-4dd3-87e9-8c130e45eaac	6cdb2e2a-e930-4208-88b6-a812c25ad9d9	4c0b79b9-9431-4ebb-8119-e33079e927f9	20	Uống 1 viên khi sốt trên 38.5 độ C	\N	2025-10-03 00:13:00.312968+07
c5983cae-1c09-4680-8c9f-d9fb76645529	6cdb2e2a-e930-4208-88b6-a812c25ad9d9	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	14	Uống 1 viên/lần, 2 lần/ngày sau ăn, dùng trong 7 ngày	\N	2025-10-03 00:13:00.312968+07
5aab90dc-baee-46c0-9742-a106c48b7ff9	f6eba578-6750-4e89-b6b5-6dd9cd052813	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	8	Uống 1 viên/lần, 2 lần/ngày sau ăn, dùng trong 7 ngày	\N	2025-10-04 18:19:17.694217+07
1099d2f6-33e4-4fd9-b5b2-46642c48e203	196db182-c70c-4313-a744-83f52d8822be	bf7c7ee3-d0db-4353-b64b-3fa43c1639ff	5	Uống 1 viên/lần, 2 lần/ngày sau ăn, dùng trong 7 ngày	\N	2025-10-06 14:39:57.477924+07
bd1553c5-7b85-4a82-a216-9d9133847e7e	31862511-30d8-4b48-aebc-ffe1eba8165c	4c0b79b9-9431-4ebb-8119-e33079e927f9	20	Uống 1 viên khi sốt	\N	2025-10-12 09:44:42.949102+07
2e99ade1-b8bd-4798-87c3-bf2caf8d1221	31862511-30d8-4b48-aebc-ffe1eba8165c	bf7c7ee3-d0db-4353-b64b-3fa43c1639ff	30	Ngày uống 1 viên sau ăn sáng	\N	2025-10-12 09:44:42.949102+07
1143ba9a-a4db-4dad-999b-c6dafec4f3e5	84edb7c8-37a0-4e9e-acc6-73dabcbf377c	bf7c7ee3-d0db-4353-b64b-3fa43c1639ff	2	Uống 1 viên/lần, 2 lần/ngày sau ăn, dùng trong 7 ngày	\N	2025-10-24 17:12:34.661328+07
c3d03318-4cab-48d4-b64d-4b20bd2f6a9d	bf2dc4ac-b765-4c03-9f4e-ed2c75679d7c	4c0b79b9-9431-4ebb-8119-e33079e927f9	20	Uống 1 viên khi sốt	\N	2025-11-27 20:24:25.745335+07
9cf9ae08-e87d-4289-9898-ebda79d26d08	bf2dc4ac-b765-4c03-9f4e-ed2c75679d7c	bf7c7ee3-d0db-4353-b64b-3fa43c1639ff	30	Ngày uống 1 viên sau ăn sáng	\N	2025-11-27 20:24:25.745335+07
ee969313-4cf2-4b28-823e-1b1b87f0a585	2aef29ca-6029-4be9-b5a6-299c3c94edb9	72ea782c-806c-48da-8d1d-5d5c8fea9351	5	1	\N	2025-12-10 11:56:58.689934+07
7a7b1429-c487-4944-ad6a-5d7b6fd3e1b8	2aef29ca-6029-4be9-b5a6-299c3c94edb9	97f3dd1a-c32a-41a5-9fb6-23062a40ff67	5	1	\N	2025-12-10 11:56:58.689934+07
\.


--
-- TOC entry 5107 (class 0 OID 312447)
-- Dependencies: 230
-- Data for Name: prescription_template_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prescription_template_items (id, template_id, product_id, quantity, dosage) FROM stdin;
53705d72-d05f-4bcf-a024-971fb5833bdc	450fb688-a978-47e0-878a-5e2cea1c1800	4c0b79b9-9431-4ebb-8119-e33079e927f9	20	Uống 1 viên khi sốt
1b206028-2a8d-4f1f-8abe-27057a651a4d	450fb688-a978-47e0-878a-5e2cea1c1800	bf7c7ee3-d0db-4353-b64b-3fa43c1639ff	30	Ngày uống 1 viên sau ăn sáng
\.


--
-- TOC entry 5098 (class 0 OID 303962)
-- Dependencies: 221
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.products (id, product_name, description, price, is_active, created_at, updated_at, product_type, category, image_url) FROM stdin;
d17e1038-d806-491a-ab11-1fc6cb20d1d7	Găng tay y tế Latex	Hộp 100 chiếc	80000.00	t	2025-10-06 10:27:01.640888+07	2025-10-06 10:27:01.640888+07	SUPPLY	\N	\N
141dbe25-88a7-4219-a680-5cc2601a88cc	Găng tay y tế Latex Vglove	Hộp 100 chiếc, không bột	90000.00	t	2025-10-06 10:33:12.406121+07	2025-10-06 10:33:12.406121+07	SUPPLY	\N	\N
6e7df233-4a39-4780-86c5-0e9d2cfaba98	Gạc y tế tiệt trùng Bảo Thạch	Túi 10 miếng, kích thước 10cm x 10cm	12000.00	t	2025-10-06 10:33:15.623158+07	2025-10-06 10:33:15.623158+07	SUPPLY	\N	\N
c133bb4a-fa68-43a6-8871-b12ae3e01128	Cồn 70 độ Vĩnh Phúc	Chai 500ml, dùng để sát khuẩn ngoài da	25000.00	t	2025-10-06 10:33:18.275922+07	2025-10-06 10:33:18.275922+07	SUPPLY	\N	\N
72ea782c-806c-48da-8d1d-5d5c8fea9351	Thuốc ho Prospan	Chai 100ml	75000.00	t	2025-10-06 10:27:42.670625+07	2025-10-28 21:41:32.619626+07	MEDICINE	Hô hấp	https://res.cloudinary.com/denuncko3/image/upload/v1761662462/tkypsxxkjah5y55xmks9.jpg
bf7c7ee3-d0db-4353-b64b-3fa43c1639ff	Vitamin C 500mg	Lọ 100 viên nén, tăng cường sức đề kháng	55000.00	t	2025-10-06 10:32:57.156052+07	2025-10-28 21:41:56.475177+07	MEDICINE	Thực phẩm chức năng	https://res.cloudinary.com/denuncko3/image/upload/v1761662485/ccytrug35zbj10rehbob.jpg
97f3dd1a-c32a-41a5-9fb6-23062a40ff67	Thuốc kháng sinh	Hộp 50 vỉ x 10 viên	10000.00	t	2025-10-02 17:28:35.507242+07	2025-10-28 21:40:33.938915+07	MEDICINE	Phổ thông	https://res.cloudinary.com/denuncko3/image/upload/v1761662403/ejm6j7z3gq6rgkyz0r4u.jpg
4c0b79b9-9431-4ebb-8119-e33079e927f9	Thuốc giảm đau Paracetamol 500mg	Hộp 10 vỉ x 10 viên	30000.00	t	2025-10-02 15:15:12.810503+07	2025-10-28 16:49:37.869551+07	MEDICINE	Phổ thông	https://res.cloudinary.com/denuncko3/image/upload/v1761644946/mtct6yoxqkaxfo5z2fia.jpg
\.


--
-- TOC entry 5110 (class 0 OID 312490)
-- Dependencies: 233
-- Data for Name: protocol_services; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.protocol_services (protocol_id, service_id) FROM stdin;
ab60ec0f-1f57-49a6-827f-4b0016fba023	fffa8048-011a-4cf1-aa37-6194bba43ac1
\.


--
-- TOC entry 5109 (class 0 OID 312484)
-- Dependencies: 232
-- Data for Name: protocol_tracking; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.protocol_tracking (id, completed_sessions, last_updated_at, patient_id, protocol_service_id, start_date, status, total_sessions) FROM stdin;
bf509068-1b09-4189-bc60-e5a5dc0dc537	0	2025-10-12 18:33:42.570225+07	7850fd0a-3a80-4afd-9de2-c16136880f3d	ab60ec0f-1f57-49a6-827f-4b0016fba023	2025-10-12 18:33:42.570225+07	IN_PROGRESS	5
\.


--
-- TOC entry 5108 (class 0 OID 312473)
-- Dependencies: 231
-- Data for Name: protocols; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.protocols (id, protocol_name, description, total_sessions, price, is_active, created_at, updated_at) FROM stdin;
ab60ec0f-1f57-49a6-827f-4b0016fba023	Liệu trình trị mụn chuyên sâu	Gồm 5 buổi, mỗi buổi cách nhau 1 tuần.	5	2500000.00	t	2025-10-12 16:39:52.87951+07	2025-10-12 16:39:52.87951+07
\.


--
-- TOC entry 5103 (class 0 OID 304050)
-- Dependencies: 226
-- Data for Name: reviews; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.reviews (id, appointment_id, patient_id, rating, comment, created_at, doctor_id, service_id) FROM stdin;
8d93a59a-f2d7-4197-8270-3f36e61cb6b2	73f18a1d-6209-4c5b-a337-dfe370b64d66	5b69f9a8-cf5b-4b7f-9f8f-30996f52452b	5	Bác sĩ rất nhiệt tình và chuyên nghiệp!	2025-10-06 22:35:19.203229+07	44ec66f7-8ab0-4e65-b47e-f11df325d938	91fd3ee3-060f-4957-b7f9-b983e01c4d4d
\.


--
-- TOC entry 5104 (class 0 OID 304070)
-- Dependencies: 227
-- Data for Name: service_materials; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.service_materials (service_id, product_id, quantity_consumed) FROM stdin;
1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	141dbe25-88a7-4219-a680-5cc2601a88cc	2
fffa8048-011a-4cf1-aa37-6194bba43ac1	6e7df233-4a39-4780-86c5-0e9d2cfaba98	1
91fd3ee3-060f-4957-b7f9-b983e01c4d4d	c133bb4a-fa68-43a6-8871-b12ae3e01128	2
\.


--
-- TOC entry 5099 (class 0 OID 303973)
-- Dependencies: 222
-- Data for Name: services; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.services (id, service_name, description, price, is_active, created_at, updated_at) FROM stdin;
91fd3ee3-060f-4957-b7f9-b983e01c4d4d	Khám Răng Tổng Quát	Kiểm tra sức khỏe răng miệng, cạo vôi và đánh bóng.	250000.00	t	2025-09-30 22:16:12.50951+07	2025-09-30 22:16:12.50951+07
fffa8048-011a-4cf1-aa37-6194bba43ac1	Khám Chân	Kiểm tra chân.	550000.00	t	2025-09-30 22:16:48.357097+07	2025-09-30 22:16:48.357097+07
1d2d4f7f-4391-473c-a03a-3fb7bd41e6b8	Khám hói tóc	Khám xem tóc có hói không.	500000.00	t	2025-10-03 10:34:32.260559+07	2025-10-03 10:34:32.260559+07
9b28db0b-bf94-40b0-b7dc-067eee92bdea	Phục hồi da	Phục hồi hư tổn da	1000000.00	t	2025-10-13 14:42:15.06225+07	2025-10-13 14:42:15.06225+07
\.


--
-- TOC entry 5096 (class 0 OID 303930)
-- Dependencies: 219
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, full_name, email, phone_number, password_hash, role, branch_id, is_active, created_at, updated_at, avatar_public_id, avatar_url) FROM stdin;
2b3b31ee-cc2e-449c-81dd-e19dbac3a837	Sơn Tùng MTP	sontungmtp.com	0987654321	$2a$10$pX6MvGIjk2Jhe8UHfFaNz.gu1CeTmz4O/u.DNFse8DQN0wDklDGaG	patient	\N	t	2025-09-29 23:33:04.073967+07	2025-09-29 23:33:04.073967+07	\N	\N
02ea0698-883b-4e53-8f06-f646fda5679e	Sơn Tùng MTP	sontungmtp111.com	0987654311	$2a$10$HFbvhGOqOyHT0F6XBn4bce9ZTKLvolqmgRO7Po9gOzTVUov82eq9S	patient	\N	t	2025-09-29 23:43:57.523897+07	2025-09-29 23:43:57.523897+07	\N	\N
7850fd0a-3a80-4afd-9de2-c16136880f3d	Me123	me.com	0127654315	$2a$10$S4ZAoQvF04mvnup4V/XAi.rYRScjSXl/0WMqeLTHxEn8pn86xw6Bq	patient	\N	t	2025-10-01 14:12:01.883131+07	2025-10-01 14:12:01.883131+07	\N	\N
44ec66f7-8ab0-4e65-b47e-f11df325d938	BS. Lê Văn Cường (Trưởng khoa)	drstrange@gmail.com	0912345629	$2a$10$VW0pAqpamubFyl6zu6TEJOvv/pizUgSh4Syul644W4We30Pncdj2u	doctor	649847b6-3def-4a78-bbe1-f480b4bbbfaf	t	2025-10-01 14:16:18.912439+07	2025-12-13 12:04:16.075513+07	avatars/44ec66f7-8ab0-4e65-b47e-f11df325d938/ateedsrpnkc8euaebgiu	https://res.cloudinary.com/denuncko3/image/upload/v1765602254/avatars/44ec66f7-8ab0-4e65-b47e-f11df325d938/ateedsrpnkc8euaebgiu.jpg
64ffbdb3-d785-4c50-a236-393b89f821e2	test	test.com	0127654215	$2a$10$XRNUs1chQuwGi6f6zXfeqOIvGuzf1AEx0OYgNIWrD6IFfmevYLFMm	patient	\N	t	2025-10-03 12:00:27.277227+07	2025-10-03 12:00:27.277227+07	\N	\N
1ddc25a5-fef6-4c99-a155-14612a280dca	staff2	staff2.com	0761235629	$2a$10$PC6RE4FdUKNQ6BIfsV9jVes./DQ3y/XV53g6/OxdHE2yLms3gGsqq	staff	649847b6-3def-4a78-bbe1-f480b4bbbfaf	t	2025-10-04 22:54:41.709708+07	2025-10-04 22:54:41.709708+07	\N	\N
b8d9500e-7c02-4f83-adf0-dda534a37a22	staff3	staff3.com	0761254629	$2a$10$bikROUrfQ3nOEaSyTRuAgucQlUoicCzjxe6TeYgiYj17lGsOcqDYq	staff	649847b6-3def-4a78-bbe1-f480b4bbbfaf	t	2025-10-04 22:54:47.378681+07	2025-10-04 22:54:47.378681+07	\N	\N
613965fd-be3e-4ffa-aae8-e47ae84a3221	p1	p1.com	0127214215	$2a$10$D6xENhqhhzCY8l/jh4oxn.WihvCeXLLKS4KHsHJjfW6ZXDZF42c/2	patient	\N	t	2025-10-04 22:55:00.728672+07	2025-10-04 22:55:00.728672+07	\N	\N
a0a3502e-5418-4313-b62a-c6cf38b47cc8	Super Admin	admin.com	\N	$2a$10$gxhVryI5SSGv9wclca/WxuYGc.GRyVQ1.BnabixtAvWthKEUSUpim	ADMIN	\N	t	2025-10-05 00:04:31.980912+07	2025-10-05 00:04:31.980912+07	\N	\N
567bb94e-ec65-404a-8abd-4382b27b6141	test1	gungdeptraivcl.com	0127214265	$2a$10$4PRy0c64kBjBrpXisy3UzO9AoW/oPEhlOlNN8gNHf4cIoFLyfT1xy	patient	\N	t	2025-10-06 23:10:31.391016+07	2025-10-06 23:10:31.391016+07	\N	\N
f9d41674-b80b-4f01-9da7-5ce480cf1344	Super Admin	admin@gmail.com	\N	$2a$10$G2V8ZM/E7IfiUrt1yehxZ.5hmoAUglFA0E0smCTlX1tKRRkQh2X.O	ADMIN	\N	t	2025-10-29 22:27:40.461196+07	2025-10-29 22:27:40.461196+07	\N	\N
5b69f9a8-cf5b-4b7f-9f8f-30996f52452b	Mono	mono.com	0287654311	$2a$10$guL4EC9wNX1q7onjX4rFTe4k.1XsOu/ALvkjWTBOZwG29WehMSC9C	patient	\N	f	2025-09-30 11:28:45.007236+07	2025-11-03 16:09:34.076288+07	\N	\N
cc12523a-78a8-4be4-8ce9-34d31ba3b3fa	Monono	mono1.com	0127654311	$2a$10$.Ys9HunvUcdBKByzRRdJZuaBcCTSEmPnbnJCJKUFkxf7JLMZy8aE2	patient	\N	f	2025-09-30 21:43:17.381078+07	2025-11-03 16:10:08.380576+07	\N	\N
db969247-58a1-447d-a247-88582eedbfcd	doc	doc@gmail.com	0761253349	$2a$10$UQnJwvb6SlZQgLAvlYJNZeHSXoFpWTYQITiMGxD4ZoOsfPwYW1IEy	doctor	649847b6-3def-4a78-bbe1-f480b4bbbfaf	f	2025-10-28 11:18:07.791522+07	2025-11-03 16:37:35.795227+07	\N	\N
a189e205-4a21-496b-948d-f610ffc08f23	test2	gungdeptraivcl@gmail.com	0127224265	$2a$10$rG2bUnRmicN4madvVC1ncuRltI7jzAfxw.jHuF47MoZdMcvYdNNWe	patient	\N	t	2025-10-06 23:11:06.593843+07	2025-12-02 00:31:49.583439+07	avatars/a189e205-4a21-496b-948d-f610ffc08f23/emg0hbn1pyprwcyp0ego	https://res.cloudinary.com/denuncko3/image/upload/v1764610307/avatars/a189e205-4a21-496b-948d-f610ffc08f23/emg0hbn1pyprwcyp0ego.jpg
11eaee07-3b27-4821-80c8-46bfeaf4a0b1	Dr T	drT@gmail.com	0462345629	$2a$10$Gh9yCtJ6QtawCUgUnUpD0eqgSgMZDhAcR42Rju12gHG9AJyTxtMnq	doctor	649847b6-3def-4a78-bbe1-f480b4bbbfaf	t	2025-10-03 10:31:19.514326+07	2025-10-03 10:31:19.514326+07	\N	\N
1ceff163-ff36-4709-9af9-44292a9a5418	staff1	staff1@gmail.com	0762345629	$2a$10$KatdcEvbGE5zaGd26T9IAetf60OpJyE8fT7Nu5qSFHAFHMd15bmgm	staff	649847b6-3def-4a78-bbe1-f480b4bbbfaf	t	2025-10-04 22:54:32.622794+07	2025-10-04 22:54:32.622794+07	\N	\N
b0d87dbd-fe60-463a-b7ed-ed53770082a7	Dr. Doom	drdoom@gmail.com	0962345629	$2a$10$oUhWESGC5xuHDR9c5YGnr.Lw/01PwdRkPRiH7GnpYU/yIGCtWNmj.	doctor	649847b6-3def-4a78-bbe1-f480b4bbbfaf	t	2025-10-03 10:30:54.558618+07	2025-10-03 10:30:54.558618+07	\N	\N
\.


--
-- TOC entry 5131 (class 0 OID 0)
-- Dependencies: 243
-- Name: chat_message_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_message_id_seq', 100, true);


--
-- TOC entry 5132 (class 0 OID 0)
-- Dependencies: 241
-- Name: chat_participant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_participant_id_seq', 122, true);


--
-- TOC entry 5133 (class 0 OID 0)
-- Dependencies: 239
-- Name: chat_room_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_room_id_seq', 61, true);


--
-- TOC entry 4887 (class 2606 OID 304011)
-- Name: appointments appointments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);


--
-- TOC entry 4920 (class 2606 OID 312892)
-- Name: bill_lines bill_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bill_lines
    ADD CONSTRAINT bill_lines_pkey PRIMARY KEY (id);


--
-- TOC entry 4916 (class 2606 OID 312871)
-- Name: bills bills_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_pkey PRIMARY KEY (id);


--
-- TOC entry 4871 (class 2606 OID 303929)
-- Name: branches branches_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.branches
    ADD CONSTRAINT branches_pkey PRIMARY KEY (id);


--
-- TOC entry 4927 (class 2606 OID 321243)
-- Name: chat_message chat_message_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_message
    ADD CONSTRAINT chat_message_pkey PRIMARY KEY (id);


--
-- TOC entry 4925 (class 2606 OID 321230)
-- Name: chat_participant chat_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_participant
    ADD CONSTRAINT chat_participant_pkey PRIMARY KEY (id);


--
-- TOC entry 4923 (class 2606 OID 321222)
-- Name: chat_room chat_room_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_room
    ADD CONSTRAINT chat_room_pkey PRIMARY KEY (id);


--
-- TOC entry 4899 (class 2606 OID 312446)
-- Name: diagnosis_templates diagnosis_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.diagnosis_templates
    ADD CONSTRAINT diagnosis_templates_pkey PRIMARY KEY (id);


--
-- TOC entry 4914 (class 2606 OID 312653)
-- Name: doctor_profiles doctor_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT doctor_profiles_pkey PRIMARY KEY (user_id);


--
-- TOC entry 4885 (class 2606 OID 303990)
-- Name: inventory inventory_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inventory
    ADD CONSTRAINT inventory_pkey PRIMARY KEY (product_id, branch_id);


--
-- TOC entry 4912 (class 2606 OID 312625)
-- Name: medical_record_services medical_record_services_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.medical_record_services
    ADD CONSTRAINT medical_record_services_pkey PRIMARY KEY (medical_record_id, service_id);


--
-- TOC entry 4889 (class 2606 OID 304044)
-- Name: medical_records medical_records_appointment_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.medical_records
    ADD CONSTRAINT medical_records_appointment_id_key UNIQUE (appointment_id);


--
-- TOC entry 4891 (class 2606 OID 304042)
-- Name: medical_records medical_records_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.medical_records
    ADD CONSTRAINT medical_records_pkey PRIMARY KEY (id);


--
-- TOC entry 4879 (class 2606 OID 303956)
-- Name: patient_profiles patient_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT patient_profiles_pkey PRIMARY KEY (user_id);


--
-- TOC entry 4910 (class 2606 OID 312522)
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- TOC entry 4897 (class 2606 OID 304195)
-- Name: prescription_items prescription_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prescription_items
    ADD CONSTRAINT prescription_items_pkey PRIMARY KEY (id);


--
-- TOC entry 4901 (class 2606 OID 312452)
-- Name: prescription_template_items prescription_template_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prescription_template_items
    ADD CONSTRAINT prescription_template_items_pkey PRIMARY KEY (id);


--
-- TOC entry 4881 (class 2606 OID 303972)
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- TOC entry 4907 (class 2606 OID 312494)
-- Name: protocol_services protocol_services_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.protocol_services
    ADD CONSTRAINT protocol_services_pkey PRIMARY KEY (protocol_id, service_id);


--
-- TOC entry 4905 (class 2606 OID 312489)
-- Name: protocol_tracking protocol_tracking_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.protocol_tracking
    ADD CONSTRAINT protocol_tracking_pkey PRIMARY KEY (id);


--
-- TOC entry 4903 (class 2606 OID 312483)
-- Name: protocols protocols_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.protocols
    ADD CONSTRAINT protocols_pkey PRIMARY KEY (id);


--
-- TOC entry 4893 (class 2606 OID 304059)
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- TOC entry 4895 (class 2606 OID 304074)
-- Name: service_materials service_materials_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_materials
    ADD CONSTRAINT service_materials_pkey PRIMARY KEY (service_id, product_id);


--
-- TOC entry 4883 (class 2606 OID 303983)
-- Name: services services_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_pkey PRIMARY KEY (id);


--
-- TOC entry 4873 (class 2606 OID 303942)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 4875 (class 2606 OID 304134)
-- Name: users users_phone_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_phone_number_key UNIQUE (phone_number);


--
-- TOC entry 4877 (class 2606 OID 303940)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 4921 (class 1259 OID 312898)
-- Name: idx_bill_lines_bill_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bill_lines_bill_id ON public.bill_lines USING btree (bill_id);


--
-- TOC entry 4917 (class 1259 OID 312884)
-- Name: idx_bills_branch_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bills_branch_id ON public.bills USING btree (branch_id);


--
-- TOC entry 4918 (class 1259 OID 312885)
-- Name: idx_bills_patient_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bills_patient_id ON public.bills USING btree (patient_id);


--
-- TOC entry 4928 (class 1259 OID 321244)
-- Name: idx_chat_message_room_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_message_room_created ON public.chat_message USING btree (room_id, created_at DESC);


--
-- TOC entry 4908 (class 1259 OID 312899)
-- Name: idx_payments_bill_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_bill_id ON public.payments USING btree (bill_id);


--
-- TOC entry 4933 (class 2606 OID 304027)
-- Name: appointments appointments_branch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_branch_id_fkey FOREIGN KEY (branch_id) REFERENCES public.branches(id);


--
-- TOC entry 4934 (class 2606 OID 304017)
-- Name: appointments appointments_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.users(id);


--
-- TOC entry 4935 (class 2606 OID 304012)
-- Name: appointments appointments_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.users(id);


--
-- TOC entry 4936 (class 2606 OID 304022)
-- Name: appointments appointments_service_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_service_id_fkey FOREIGN KEY (service_id) REFERENCES public.services(id);


--
-- TOC entry 4949 (class 2606 OID 312893)
-- Name: bill_lines bill_lines_bill_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bill_lines
    ADD CONSTRAINT bill_lines_bill_id_fkey FOREIGN KEY (bill_id) REFERENCES public.bills(id) ON DELETE CASCADE;


--
-- TOC entry 4948 (class 2606 OID 312654)
-- Name: doctor_profiles fk_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 4931 (class 2606 OID 303996)
-- Name: inventory inventory_branch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inventory
    ADD CONSTRAINT inventory_branch_id_fkey FOREIGN KEY (branch_id) REFERENCES public.branches(id);


--
-- TOC entry 4932 (class 2606 OID 303991)
-- Name: inventory inventory_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inventory
    ADD CONSTRAINT inventory_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- TOC entry 4947 (class 2606 OID 312626)
-- Name: medical_record_services medical_record_services_medical_record_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.medical_record_services
    ADD CONSTRAINT medical_record_services_medical_record_id_fkey FOREIGN KEY (medical_record_id) REFERENCES public.medical_records(id);


--
-- TOC entry 4937 (class 2606 OID 304045)
-- Name: medical_records medical_records_appointment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.medical_records
    ADD CONSTRAINT medical_records_appointment_id_fkey FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);


--
-- TOC entry 4930 (class 2606 OID 303957)
-- Name: patient_profiles patient_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT patient_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 4946 (class 2606 OID 312523)
-- Name: payments payments_appointment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_appointment_id_fkey FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);


--
-- TOC entry 4942 (class 2606 OID 304196)
-- Name: prescription_items prescription_items_medical_record_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prescription_items
    ADD CONSTRAINT prescription_items_medical_record_id_fkey FOREIGN KEY (medical_record_id) REFERENCES public.medical_records(id);


--
-- TOC entry 4943 (class 2606 OID 312453)
-- Name: prescription_template_items prescription_template_items_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prescription_template_items
    ADD CONSTRAINT prescription_template_items_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.diagnosis_templates(id);


--
-- TOC entry 4944 (class 2606 OID 312495)
-- Name: protocol_services protocol_services_protocol_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.protocol_services
    ADD CONSTRAINT protocol_services_protocol_id_fkey FOREIGN KEY (protocol_id) REFERENCES public.protocols(id);


--
-- TOC entry 4945 (class 2606 OID 312500)
-- Name: protocol_services protocol_services_service_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.protocol_services
    ADD CONSTRAINT protocol_services_service_id_fkey FOREIGN KEY (service_id) REFERENCES public.services(id);


--
-- TOC entry 4938 (class 2606 OID 304060)
-- Name: reviews reviews_appointment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_appointment_id_fkey FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);


--
-- TOC entry 4939 (class 2606 OID 304065)
-- Name: reviews reviews_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.users(id);


--
-- TOC entry 4940 (class 2606 OID 304080)
-- Name: service_materials service_materials_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_materials
    ADD CONSTRAINT service_materials_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- TOC entry 4941 (class 2606 OID 304075)
-- Name: service_materials service_materials_service_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_materials
    ADD CONSTRAINT service_materials_service_id_fkey FOREIGN KEY (service_id) REFERENCES public.services(id);


--
-- TOC entry 4929 (class 2606 OID 303945)
-- Name: users users_branch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_branch_id_fkey FOREIGN KEY (branch_id) REFERENCES public.branches(id);


-- Completed on 2025-12-16 22:01:41

--
-- PostgreSQL database dump complete
--

