from sshtunnel import SSHTunnelForwarder
import psycopg2

def get_aws_connection():
    '''
    Returns a connection to the hosted AWS database p2-database
    It does this by first creating an ssh tunnel into an EC2 instance, which then connects to the database.
    '''

    # Configuration Variables
    EC2_PUBLIC_IP = "13.59.236.25"  # EC2 Public IP
    EC2_USER = "ec2-user"  #'ec2-user' for Amazon Linux
    PATH_TO_PEM_KEY = "secret\\p2-database.pem"

    PRIVATE_TARGET_HOST = "database-p2.cx2cyck8swfj.us-east-2.rds.amazonaws.com"
    PRIVATE_TARGET_PORT = 5432  # base PostgreSQL port


    # Setup the tunnel forwarder
    server = SSHTunnelForwarder(
        (EC2_PUBLIC_IP, 22),
        ssh_username=EC2_USER,
        ssh_pkey=PATH_TO_PEM_KEY,
        remote_bind_address=(PRIVATE_TARGET_HOST, PRIVATE_TARGET_PORT),
        local_bind_address=("127.0.0.1", 10000),  # Map to an open local port
        ssh_password="tW83Wfee7NQaWp47h0eZ"
    )
    server.start()
    connection = psycopg2.connect(
        host="127.0.0.1",
        database="postgres",
        user="postgres",
        password="tW83Wfee7NQaWp47h0eZ",
        port=server.local_bind_port
        )
    return connection