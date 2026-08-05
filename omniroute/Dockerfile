FROM omniroute/omniroute:latest

# Expose the OmniRoute API port
EXPOSE 20128

# Set up volume for persisting OmniRoute configuration (combos, keys)
VOLUME ["/app/data"]

# Run the OmniRoute server (assuming serve command is default)
CMD ["serve", "--host", "0.0.0.0", "--port", "20128", "--data-dir", "/app/data"]
