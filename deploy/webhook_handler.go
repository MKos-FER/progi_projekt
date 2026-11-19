
package main

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	defaultPort       = "9000"
	urlPath           = "/github_webhook"
	maxPayloadBytes   = 1 << 20 // 1 MiB
	deploymentTimeout = 10 * time.Minute
)

var deployScript = "/opt/PROGI_deployment/deploy.sh"

type PushEvent struct {
	Ref string `json:"ref"`
}

type Server struct {
	secret string
	deployMu sync.Mutex
}

func main() {
	secret := os.Getenv("GITHUB_WEBHOOK_SECRET")
	if secret == "" {
		log.Fatal("GITHUB_WEBHOOK_SECRET must be set")
	}

	script := os.Getenv("DEPLOY_SCRIPT_PATH")
	if script != "" {
		println("Using deploy script: ", script)
		deployScript = script
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = defaultPort
	}

	portNumber, err := strconv.Atoi(port)
	if err != nil || portNumber < 1 || portNumber > 65535 {
		log.Fatal("PORT must be a valid TCP port between 1 and 65535")
	}

	server := &Server{secret: secret}

	mux := http.NewServeMux()
	mux.HandleFunc(urlPath, server.handleWebhook)

	addr := net.JoinHostPort("127.0.0.1", strconv.Itoa(portNumber))
	httpServer := &http.Server{
		Addr:              addr,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      deploymentTimeout + time.Minute,
		IdleTimeout:       60 * time.Second,
		MaxHeaderBytes:    16 << 10,
	}

	log.Printf("Webhook server listening on %s\n", addr)
	log.Fatal(httpServer.ListenAndServe())
}

func (s *Server) handleWebhook(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	if r.URL.Path != urlPath {
		http.NotFound(w, r)
		return
	}

	log.Println("Webhook received!")

	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, maxPayloadBytes))
	if err != nil {
		var maxErr *http.MaxBytesError
		if errors.As(err, &maxErr) {
			http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
		} else {
			http.Error(w, "Could not read payload", http.StatusBadRequest)
		}
		return
	}

	if !s.validSignature(body, r.Header.Get("X-Hub-Signature-256")) {
		http.Error(w, "Invalid signature", http.StatusUnauthorized)
		return
	}

	if r.Header.Get("X-GitHub-Event") == "ping" {
		log.Println("Webhook ignored: not a push event")
		http.Error(w, "Pong! - The webhook server is responsive", http.StatusAccepted)
		return
	}

	if r.Header.Get("X-GitHub-Event") != "push" {
		log.Println("Webhook ignored: not a push event")
		http.Error(w, "Ignored event", http.StatusAccepted)
		return
	}

	var event PushEvent
	if err := json.Unmarshal(body, &event); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	if event.Ref != "refs/heads/master" {
		log.Printf("Webhook ignored: push to %s\n", event.Ref)
		http.Error(w, "Ignored branch", http.StatusAccepted)
		return
	}

	// Prevent concurrent deployments from overlapping.
	s.deployMu.Lock()
	defer s.deployMu.Unlock()

	ctx, cancel := context.WithTimeout(
		context.Background(), deploymentTimeout,
	)
	defer cancel()

	cmd := exec.CommandContext(ctx, deployScript)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	log.Println("Starting deployment for master")

	if err := cmd.Run(); err != nil {
		if ctx.Err() != nil {
			log.Printf("Deployment timed out or was cancelled: %v\n", ctx.Err())
		} else {
			log.Printf("Deployment failed: %v\n", err)
		}
		http.Error(w, "Deployment failed", http.StatusInternalServerError)
		return
	}

	log.Println("Deployment completed successfully")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("Deployment successful"))
}

func (s *Server) validSignature(body []byte, signature string) bool {
	if !strings.HasPrefix(signature, "sha256=") {
		return false
	}

	provided, err := hex.DecodeString(strings.TrimPrefix(signature, "sha256="))
	if err != nil {
		return false
	}

	mac := hmac.New(sha256.New, []byte(s.secret))
	_, _ = mac.Write(body)
	expected := mac.Sum(nil)

	return hmac.Equal(provided, expected)
}

var _ = fmt.Sprintf

