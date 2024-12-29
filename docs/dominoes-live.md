Forget overviews from 60,000 feet or 30,000 feet. Now we're at 0 feet. 

We're writing code for a real app - live, in this document.


## Which App?

The "simple" one. 

Within the re-frame repository's `/examples` folder is an app 
called [/simple](https://github.com/day8/re-frame/tree/master/examples/simple).

It has 70 lines of code in [a single namespace](https://github.com/day8/re-frame/blob/master/examples/simple/src/simple/core.cljs).
Below, we'll look at all 70 lines.

!!! Info ""
    Below, you should see the app running live. <br>
    Try changing the display colour to `magenta`, `#f00` or `#02a6f2`.

<div id="dominoes-live-app">
  <div class="preload">  
    The live application should start here ...
    <br><br>
    Doesn't work? Maybe try disabling your adblocker for this site. 

  
  </div>
</div>

!!! Info ""
    When you change the live code on this page, the app will change.
    This live coding is powered by [SCI](https://github.com/borkdude/sci).


## The Namespace

Within our single namespace (called `re-frame.simple`), we'll need to use both `reagent` and `re-frame`. 
So, at the top, within the `ns`, we'll give the name and require the other namespaces we'll need: 
<div class="cm-doc">
(ns re-frame.simple
  (:require [reagent.dom.client :as rdc]
            [re-frame.core :as rf]))
</div>

!!! Note "Live Code Fragment"
    Above, the code is provided within an editor. You can change the code if you want, and then press the "eval" button. 
    Below the editor, you'll see a green-backgrounded box. If the code was evaluated successfully a tick is shown along with the value of that evaluation.
    In the case of our `ns` form, it evaluates to `nil`, which is not very interesting.

## The Data Schema

Let's talk `app-db`. Now, usually, I'd recommend that you write a schema
for your application state, but here, we'll cut that corner to minimise cognitive load.

But we can't cut it completely. You'll still need an
informal description, and here it is ... `app-db` will contain
a two-key map like this:
```clj
{:time       (js/Date.)  ;; current time for display
 :time-color "#f88"}     ;; the colour in which the time should be shown
```

## Event Dispatch

Events are data.

re-frame uses a vector format for events. For example:
```clj
[:time-color-change "red"]
```

The first element in the vector is a keyword that identifies the `kind` of `event`.
Further elements are optional and can provide additional data
associated with the event. The additional value above, `"red"`, is
presumably the colour.

Here are some other example events:
```clj
[:admit-to-being-satoshi false]
[:dressing/put-pants-on  "velour flares" {:method :left-leg-first :belt false}]
```

For non-trivial applications, the `kind` keyword will be namespaced, as it is in the 2nd example.

### dispatch

To send an event, call `dispatch` with the event vector as the argument. 
```clj
(dispatch [:kind-of-event value1 value2])
```

For our simple app, we do this ... 
<div class="cm-doc" data-cm-doc-result-format="pass-fail">
(defn dispatch-timer-event        ;; <-- defining a function
  []                              ;; <-- no args
  (let [now (js/Date.)]           ;; <-- obtain the current time
    (rf/dispatch [:timer now])))  ;; <-- dispatch an event
</div>

Notes: 

  - `defn` returns nothing of interest, you can ignore the green box below the code.
  - the current time is obtained with `(js/Date.)` which is the equivalent of `new Date()` in javascript.
  - Within the `ns` at the top of the namespace, `re-frame` is aliased as `rf` via `[re-frame.core :as rf]`. So, the symbol `rf/dispatch` is a reference to the function `dispatch` in the re-frame API. 

<div class="cm-doc">
(defonce do-timer (js/setInterval dispatch-timer-event 1000))
</div>

Notes:

  - `setInterval` is used to call `dispatch-timer-event` every second 
  - `defonce` is like `def`, it instantiates a symbol and binds a value to it. But the evaluation won't happen if the symbol (`do-timer` in this case) already exists. This stops a new timer from getting created every time we hot-reload the code in a namespace. This would be important in a dev environment where we were editing the namespace and our changes were causing it to be recompiled and hot-code reloaded.

A timer is an unusual source of events. Usually, it is an app's UI widgets that dispatch events 
(in response to user actions), or an HTTP POST's `on-success` handler or a websocket which gets a new packet. 
So, this "simple" app is unusual. Moving on. 


### After dispatch 

`dispatch` puts an event onto a queue for processing.

So, an event is not processed synchronously, like a function call. The processing
happens later - asynchronously. Very soon, but not now.

The consumer of the queue is the re-frame `router` which looks after the event's processing.

The `router` will:

1. inspect the 1st element of an event vector
2. look up the event handler (function) **registered** for this kind of event
3. call this event handler (function) with the necessary arguments

Our job, then, is to register an event handler function for 
each kind of event, including this `:timer` event. 

## Event Handlers

Collectively, event handlers provide the control logic in a re-frame application.

In this application, three kinds of event are dispatched:

  - `:initialize` - dispatch once, when the program boots up
  - `:time-color-change` - dispatched whenever the user changes the colour text field
  - `:timer` - dispatched once a second via a timer

Having 3 events means we'll be registering 3 event handlers.

### Registering Event Handlers

Event handler functions:

  - take two arguments `coeffects` and `event`
  - return `effects`

Conceptually, you can think of the argument `coeffects` as being "the current state of the world". 
And you can think of event handlers as computing how the world should be changed 
by the arriving event. They return (as data) how the world should be changed by the event - `the side effects` of the event.

Event handlers can be registered in two ways:

  - `reg-event-fx`
  - `reg-event-db`  
  
 One ends in `-fx` and the other in `-db`.

  - `reg-event-fx` can take many `coeffects` and can return many `effects`
  - `reg-event-db` allows you to write simpler handlers for the common case where you want 
  them to take only one `coeffect` - the current app state - and return one `effect` - the 
  updated app state.

Many vs One. Because of its simplicity, we'll be using the latter one here: `reg-event-db`.

### reg-event-db

We register event handlers using re-frame's API: 

```clj
(rf/reg-event-db           ;; <-- the re-frame API function to use
  :the-event-id            ;; <-- the event id 
  the-event-handler-fn)    ;; <-- the handler function
```
The handler function you provide should expect two arguments:

   - `db` - the current application state  (the map value contained in `app-db`)
   - `e` - the event vector  (given to `dispatch`)
    
So, your handler function will have a signature like this: `(fn [db e] ...)`.

These event handlers must compute and return the new state of
the application, which means they return a modified version of `db`.


### :timer

<div class="cm-doc" data-cm-doc-result-format="pass-fail">
(rf/reg-event-db                 
  :timer
  (fn [db [_ new-time]]          ;; notice how we destructure the event vector
    (assoc db :time new-time)))  ;; compute and return the new application state
</div>

Notes:

1. the `event` (2nd parameter) will be like `[:timer a-js-time]`
2. [sequence destructuring](https://clojure.org/guides/destructuring) 
   is used to extract the 2nd element of that event vector (ignores first with `_`)
3. `db` is a map, containing two keys (see description above)
4. this handler computes a new application state from `db`, and returns it
5. it just `assocs` in the `new-time` value

### :initialize

Once on startup, application state must be initialised. We
want to put a sensible value into `app-db`, which starts out containing an empty map `{}`.

A single `(dispatch [:initialize])` will happen early in the 
app's life (more on this below), and we need to register an `event handler`
for it. 

This event handler is slightly unusual because it ignores both of its arguments. 
There's nothing in the `event` vector which it needs. Nor is the existing value in 
`db`. It just wants to plonk a completely new value into `app-db`

<div class="cm-doc" data-cm-doc-result-format="pass-fail">
(rf/reg-event-db              ;; sets up initial application state
  :initialize
  (fn [ _ _ ]                 ;; arguments not important, so use _
    {:time (js/Date.)         ;; returned value put into app-db 
     :time-color "orange"}))  ;; so the app state will be a map with two keys
</div>


For comparison, here's how we could have written this if we **did** care about the existing value in `db`: 
```clj
(rf/reg-event-db
  :initialize
  (fn [db _]                 ;; we use db this time, so name it
    (-> db                   ;; db is initially just {}
      (assoc :time (js/Date.))
      (assoc :time-color "orange")))
```


### :time-color-change

When the user enters a new colour value (into the input field) the view will `(dispatch [:time-color-change new-colour])` (more on this below). 

<div class="cm-doc" data-cm-doc-result-format="pass-fail">
(rf/reg-event-db
  :time-color-change            
  (fn [db [_ new-color-value]]
    (assoc db :time-color new-color-value)))   ;; compute and return the new application state
</div>

Notes:

1. it updates `db` to contain the new colour, provided as the 2nd element of the event 

## Effect Handlers

Domino 3 actions the `effects` returned by event handlers.

In this "simple" application, our event handlers are implicitly returning 
only one effect: "update application state". 

This particular `effect` is accomplished by a re-frame-supplied 
`effect` handler. So, there's nothing for us to do for this domino. We are 
using a standard re-frame effect handler.

And this is not unusual. You seldom write `effect` handlers. But it is covered in 
a later tutorial. 

## Subscription Handlers

Subscription handlers, or `query` functions, take application state as an argument 
and run a query over it, returning something called
a "materialised view" of that application state.

When the application state changes, subscription functions are 
re-run by re-frame, to compute new values (new materialised views).

Ultimately, the data returned by these `query` functions flow through into 
the `view` functions (Domino 5).
 
One subscription can
source data from other subscriptions. So it is possible to
create a tree structure of data flow. 
 
The Views (Domino 5) are the leaves of this tree.  The tree's 
root is `app-db` and the intermediate nodes between the two 
are computations being performed by the query functions of Domino 4.

Now, the two subscriptions below are trivial. They just extract part of the application
state and return it. So, there's virtually no computation - the materialised view they
produce is the same as that stored in `app-db`. A more interesting tree
of subscriptions, and more explanation, can be found in the todomvc example.

### reg-sub 

`reg-sub` associates a `query id` with a function that computes
 that query, like this:
```clj
(rf/reg-sub
  :some-query-id  ;; query id (used later in subscribe)
  a-query-fn)     ;; the function which will compute the query
```
Later, a view function (domino 5) will subscribe to a query like this:
`(subscribe [:some-query-id])`, and `a-query-fn` will be used 
to perform the query over the application state.

Each time application state changes, `a-query-fn` will be
called again to compute a new materialised view (a new computation over `app-db`)
and that new value will be given to all `view` functions that are subscribed
to `:some-query-id`. These `view` functions will then be called to compute the 
new DOM state (because the views depend on query results that have changed).

Along this reactive chain of dependencies, re-frame will ensure the 
necessary calls are made, at the right time.

Here's the code for defining our 2 subscription handlers:
<div class="cm-doc">
(rf/reg-sub
  :time
  (fn [db _]     ;; db is current app state. 2nd unused param is the query vector
    (:time db))) ;; return a query computation over the application state
</div>

<div class="cm-doc">
(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))
</div>

Both of these queries are trivial. They are known as "accessors", or layer 2, subscriptions. More on that soon.

## View Functions

`view` functions compute Hiccup.  They are "Data in, Hiccup out" and they are Reagent 
components. 

A SPA will have lots of `view` functions, and collectively, 
they render the app's UI.
 
## Subscribing

To render a hiccup representation of some part of the app state, view functions must query 
for that part of `app-db`, by using `subscribe`.

`subscribe` is used like this:
```clj
   (rf/subscribe  [query-id some optional query parameters])
```
So `subscribe` takes one argument, assumed to be a vector.

The first element in the vector identifies the query, 
and the other elements are optional
query parameters. With a traditional database, a query might be:
```sql
select * from customers where name="blah"
```

In re-frame, that would look like:
   `(subscribe  [:customer-query "blah"])`,
which would return a `ratom` holding the customer state (a value that might change over time!).

!!! Warning "Rookie Mistake"
    Because subscriptions return a `ratom` (a Reagent atom), they must always be dereferenced to 
    obtain the value (using [deref](https://clojuredocs.org/clojure.core/deref) or [the reader macro](https://en.wikibooks.org/wiki/Learning_Clojure/Reader_Macros)  `@`). Forgetting to do this is a recurring papercut for newbies.

## The View Functions 

This view function renders the clock:
<div class="cm-doc">
(defn clock
  []
  (let [colour @(rf/subscribe [:time-color])
        time   (some-> @(rf/subscribe [:time])
                    .toTimeString
                    (clojure.string/split " ")
                    first)]
  [:div.example-clock {:style {:color colour}} time]))
</div>

As you can see, it uses `subscribe` twice to obtain two pieces of data from `app-db`. 
If either value changes, Reagent will automatically re-run this view function, 
computing new hiccup, which means new DOM.

Using the power of `sci`, we can render just the `clock` component: 

<div class="cm-doc">
(defonce clock-root
  (rdc/create-root (js/document.getElementById "clock")))

(rdc/render clock-root [clock])
</div>
<div id="clock"></div>

When an event handler changes a value in `app-db`, `clock` will rerender. Try it. 
Edit the following code to remove the comment, and then press the Eval button to execute it. Notice the change in the clock above. Experiment. 
<div class="cm-doc">
(comment (rf/dispatch  [:time-color-change "green"]))
</div>

And this view function renders the input field:
<div class="cm-doc">
(defn color-input
  []
  (let [gettext (fn [e] (-> e .-target .-value))
        emit    (fn [e] (rf/dispatch [:time-color-change (gettext e)]))]
    [:div.color-input
      "Display color: "
      [:input {:type "text"
               :style {:border "1px solid #CCC" }
               :value @(rf/subscribe [:time-color])        ;; subscribe
               :on-change emit}]]))              ;; <---
</div>

Notice how it does BOTH a `subscribe` to obtain the current value AND 
a `dispatch` to say when it has changed (look for `emit`). 

It is very common for view functions to render event-dispatching functions into the DOM.
The user's interaction with the UI is usually a large source of events.

Notice also how we use `@` in front of `subscribe` to obtain the value out of the subscription. It is almost as if the subscription is an atom holding a value (which can change over time). 

We can render the `color-input` as any other reagent component:
<div class="cm-doc">
(defonce color-input-root
  (rdc/create-root (js/document.getElementById "color-input")))

(rdc/render color-input-root [color-input])
</div>
<div id="color-input"></div>

And then there's a parent `view` to arrange the others. It contains no 
subscriptions or dispatching of its own:
<div class="cm-doc">
(defn ui
  []
  [:div
   [:h1 "The time is now:"]
   [clock]
   [color-input]])
</div>

!!! Note ""
    `view` functions form a hierarchy, often with 
    data flowing from parent to child via
    arguments (props in React). So, not every view needs a subscription if the 
    values passed in from a parent component are sufficient.

!!! Note ""
    `view` functions never directly access `app-db`. Data is 
    only ever sourced via subscriptions. 


## Kick Starting The App

Below, the function `run` is called to kick off the application once the HTML page has loaded.

It has two tasks:

1. Load the initial application state
2. Load the GUI by "mounting" the root-level  
   `view` - in our case, `ui` -
   onto an existing DOM element (with id `app`). 

<div class="cm-doc">

(defonce dominoes-live-app-root
  (rdc/create-root (js/document.getElementById "dominoes-live-app")))
(defn mount-ui
  []
  (rdc/render dominoes-live-app-root [ui])) ;; mount the application's ui

(defn run
  []
  (rf/dispatch-sync [:initialize])     ;; puts a value into application state
  (mount-ui))
</div>

When it comes to establishing initial application state, you'll
notice the use of `dispatch-sync`, rather than `dispatch`. This is a simplifying cheat
which ensures that a correct
structure exists in `app-db` before any subscriptions or event handlers run. 

After `run` is called, the app passively waits for `events`. 
Nothing happens without an `event`.

<div class="cm-doc">
(run)
</div>

The run function renders the app in the DOM element whose id is `dominoes-live-app`: this DOM element is located at the top of the page. 
This is the element we used to show how the app looks at the top of this page

To save you the trouble of scrolling up to the top of the page, I decided to render the whole app as a 
reagent element, just here:

<div class="cm-doc">
(defonce dominoes-live-app-2-root
  (rdc/create-root (js/document.getElementById "dominoes-live-app-2")))

(rdc/render dominoes-live-app-2-root [ui])
</div>
<div id="dominoes-live-app-2"></div>

## T-Shirt Unlocked

Good news.  If you've read this far,
your insider's T-shirt will be arriving soon - it will feature turtles, 
[xkcd](http://xkcd.com/1416/) and something about "data all the way down". 
But we're still working on the hilarious caption bit. Open a
repo issue with a suggestion.

