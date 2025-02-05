# Re-signaali

This project is a fork of [Re-frame `v1.4.3`](https://github.com/day8/re-frame/tree/v1.4.3) where
the reactivity is implemented using [Signaali](https://github.com/metosin/signaali)
instead of [Reagent](https://github.com/reagent-project/reagent).

It was created:
- to battle-proof Signaali's API before its release,
- to demonstrate how Signaali could be used in future versions of Re-frame,
- for Re-frame users who already use another React wrapper than Reagent and who want to try Signaali.

Project status: [experimental](https://github.com/metosin/open-source/blob/main/project-status.md#experimental).

## How well does it work?

- The fork is working fine when tested on our own projects and on the port of Re-frame's examples.
- We didn't port Re-frame's unit tests which are still using Reagent atoms.
- Re-signaali behaves a bit differently compared to Re-frame:
  - Re-frame's event queue is bypassed.
  - Reagent's event queue is bypassed.
  - As a result, the jumping caret bug no longer exists in this fork.
  - `rf/dispatch` and `rf/dispatch-sync` are the same function.

## Ported Re-frame library

The namespaces were not modified, keep using it as if it was the original Re-frame project.

What's different in the library:
- Reagent as a reactivity provider was replaced by Signaali.

## Ported Re-frame examples

There are 3 projects in the `examples/` directory, where:
- Reagent as a React wrapper was replaced by UIx,
- Re-frame was replaced by Re-signaali.

## How to use it in your Re-frame project

In your `deps.edn`, replace the re-frame dependencies with:

```clojure
fi.metosin/re-signaali {:git/url "https://github.com/metosin/re-signaali.git"
                        :git/sha "..."}
```

This will work only if:
- You no longer use Reagent as a React wrapper.
- You no longer use Reagent's reactions or atoms.

## Will this be maintained?

No. We have no plan to maintain this project for any release of Re-frame after `v1.4.3`.

Instead, we hope that in the future Re-frame can offer a customization mechanism for its reactivity
such that no project fork is needed.

---

Below this line is the original content of Re-frame's `README.md`.

---

<p align="center"><a href="https://day8.github.io/re-frame" target="_blank" rel="noopener noreferrer"><img src="docs/images/logo/re-frame-colour.png?raw=true" alt="re-frame logo"></a></p>

## Derived Values, Flowing

> This, milord, is my family's axe. We have owned it for almost nine hundred years, see. Of course,
sometimes it needed a new blade. And sometimes it has required a new handle, new designs on the
metalwork, a little refreshing of the ornamentation ... but is this not the nine hundred-year-old
axe of my family? And because it has changed gently over time, it is still a pretty good axe,
y'know. Pretty good.

> -- Terry Pratchett, The Fifth Elephant <br>
> &nbsp;&nbsp;&nbsp; reflecting on identity, flow and derived values  (aka [The Ship of Theseus](https://en.wikipedia.org/wiki/Ship_of_Theseus))
<br/> 
<br/>

<!--
[![CI](https://github.com/day8/re-frame/workflows/ci/badge.svg)](https://github.com/day8/re-frame/actions?workflow=ci)
[![CD](https://github.com/day8/re-frame/workflows/cd/badge.svg)](https://github.com/day8/re-frame/actions?workflow=cd)
[![License](https://img.shields.io/github/license/day8/re-frame.svg)](license.txt)
-->

## Overview

re-frame is a ClojureScript framework for building user interfaces.
It has a data-oriented, functional design. Its primary focus is on high programmer productivity and scaling up to larger Single-Page applications.

Developed in late 2014, and released in 2015, it is mature and stable. It is used by both small startups and companies with over 500 developers, and it has delivered into production applications which are 40K lines of code and beyond. 

Across the last 6 years, it has outlasted multiple generations of Javascript churn - just imagine your team's productivity if you didn't have to contend with technical churn, and have new magic burn your fingers every two years. Brand new, exciting concepts like recoiljs (in the React world), have been a regular part of re-frame from the beginning. 

re-frame is lucky enough to enjoy an unfair advantage - ClojureScript is a Lisp. Alan Kay
once described Lisp as "Maxwell's equations of software". Paul Graham 
described how Lisp was a competitive advantage for his startup.  When we use Lisp, we 
get to leverage 50 years of foliated excellence from the very best minds available.
And then there's also a thriving ClojureScript community which delivers modern ideas and best-in-class tooling.

Although re-frame leverages React (via Reagent), it only needs 
React to be the V in MVC, and no more. re-frame takes a different road to the currently-pervasive idea that Views should be causal (colocated queries, ComponentDidMount, hooks, etc).
In re-frame, events are causal, and views are purely reactive. 

## Documentation 

The re-frame documentation is [available here](https://day8.github.io/re-frame/).


## The Current Version 

[![Clojars Project](https://img.shields.io/clojars/v/re-frame?labelColor=283C67&color=729AD1&style=for-the-badge&logo=clojure&logoColor=fff)](https://clojars.org/re-frame)

For full dependency information, see the [Clojars page](https://clojars.org/re-frame/)

## Getting Help 

[![Get help on Slack](http://img.shields.io/badge/slack-clojurians%20%23re--frame-97C93C?labelColor=283C67&logo=slack&style=for-the-badge)](https://clojurians.slack.com/channels/re-frame)

## Licence

re-frame is [MIT licenced](license.txt)

