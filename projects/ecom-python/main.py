from fastapi import FastAPI
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.orm import sessionmaker, declarative_base
from typing import List
import os

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://postgres:admin-user@localhost:5432/ecom_python"
)

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

class Product(Base):
    __tablename__ = "products"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)

app = FastAPI()

@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)
    session = SessionLocal()
    
    if session.query(Product).count() == 0:
        produtos = [
            "Notebook Dell Inspiron 15",
            "Smartphone Samsung Galaxy S23",
            "Fone de Ouvido JBL Tune 510BT",
            "Monitor LG UltraWide 29''",
            "Teclado Mecânico Redragon Kumara",
            "Mouse Logitech MX Master 3S",
            "Cadeira Gamer ThunderX3",
            "HD Externo Seagate 2TB",
            "Impressora HP DeskJet Ink Advantage",
            "Smartwatch Amazfit GTS 4 Mini"
        ]
        for nome in produtos:
            session.add(Product(name=nome))
        session.commit()
    session.close()

@app.get("/products", response_model=List[dict])
def get_products():
    session = SessionLocal()
    products = session.query(Product).all()
    session.close()
    return [{"id": p.id, "name": p.name} for p in products] 