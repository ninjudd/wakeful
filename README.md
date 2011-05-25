# Wakeful

Wakeful is a Restful routing alternative for Clojure. It makes it really easy to connect your web
api to namespaces in your project.

# Usage

    (use 'wakeful.core)

    (def handler (wakeful "awesome.api"))

Now http calls dispatch to methods calls in namespaces under `awesome.api`:

    GET  /photo-123/thumbnail  -> awesome.api.photo/thumbnail
    GET  /photo-123/tags       -> awesome.api.photo/tags
    POST /photo-123/tag/user-1 -> awesome.api.photo/tag!
