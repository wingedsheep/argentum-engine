package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.CardAdvisorModule
import com.wingedsheep.ai.engine.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.ai.engine.advisor.modules.OnslaughtAdvisorModule
import com.wingedsheep.ai.engine.budget.BudgetPolicy
import com.wingedsheep.ai.engine.budget.LegacyBudgetPolicy
import com.wingedsheep.ai.engine.budget.TieredBudgetPolicy
import com.wingedsheep.ai.engine.evaluation.CreatureValuation
import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.rollout.RolloutSettings

/**
 * A named, reproducible configuration of the engine AI.
 *
 * This is the versioning seam the arena measures against, and the switchboard the later phases of
 * `backlog/engine-ai-improvement.md` hang their features off (candidate evaluator, rollout counts,
 * determinizations, budget overrides). Today it carries only what is actually wired — a profile
 * field that nothing reads would be a lie about what a run measured.
 *
 * **[LEGACY_V0] is the permanent reference opponent.** Every later version reports its win rate
 * against it, so the numbers stay comparable across months. It must never be "improved"; if a
 * refactor moves it, `FrozenBaselineTest` fails, which is the whole point of that test.
 */
data class AiProfile(
    /** Stable identifier. Appears in arena reports and CSV rows; treat it as part of the API. */
    val id: String,
    /**
     * Per-set card advisor modules. Modules and the advisors they register are stateless
     * singletons, so one profile instance is safe to share across threads and games.
     */
    val advisorModules: List<CardAdvisorModule> = emptyList(),
    /** Resource-backed leaf-evaluation vector. Unknown ids use the compiled default safely. */
    val evalWeightsId: String = EvalWeights.DEFAULT_ID,
    /**
     * Phase 4a: only propose actions the AI can actually take, and skip windows where it has
     * none.
     *
     * Routes candidate generation and whole-window skipping through
     * [com.wingedsheep.engine.legalactions.MeaningfulActionFilter] — the same rules the client's
     * auto-pass uses — and fixes the Strategist's target filling to fill the slots it can instead
     * of abandoning a spell whose *optional* slot happens to be empty. Off for [LEGACY_V0]:
     * the second half is a plain bug fix, but the reference opponent has to stay frozen or every
     * number published against it silently rebases.
     */
    val useMeaningfulFilter: Boolean = false,
    /**
     * Phase 4b. How much search one decision may spend. [LegacyBudgetPolicy] is today's
     * constants with no global deadline.
     */
    val budgetPolicy: BudgetPolicy = LegacyBudgetPolicy,
    /**
     * Phase 6: structural card knowledge
     * ([com.wingedsheep.ai.engine.knowledge.CardIntent]).
     *
     * Turns on three consumers at once — `BoardPresence.permanentValue`'s flat `0.5` for every
     * non-creature permanent, `Strategist.heuristicTargetRank`'s flat `0.0` for one, and the
     * intent-driven [com.wingedsheep.ai.engine.knowledge.HoldPolicy] that replaces the hardcoded
     * end-step discount. Off for [LEGACY_V0]: the reference opponent has to stay frozen or every
     * number published against it silently rebases.
     */
    val useCardIntent: Boolean = false,
    /**
     * Phase 7: score a candidate by the mean of several short playouts instead of one static
     * evaluation. Null is the off position and leaves the greedy 1-ply leaf in place.
     *
     * This is the plan's primary strength lever, and the only phase so far whose job is to move a
     * win rate rather than enable one. Off for [LEGACY_V0] for the usual reason — the reference
     * opponent has to stay frozen — and off for [CURRENT] until the arena says it should ship.
     *
     * How *many* playouts a decision gets comes from the budget tier, not from here; see
     * [com.wingedsheep.ai.engine.budget.SearchAllowances.rolloutPlayouts]. A profile with rollouts
     * on and [LegacyBudgetPolicy] still gets them, at the nominal NORMAL count — which is what
     * makes `v0-rollout` an attributable one-variable change from `v0`.
     */
    val rollouts: RolloutSettings? = null,
    /**
     * Phase 8: sample opponent hand identities and library order before rollout evaluation.
     * Off preserves the historical full-information agents used as arena controls.
     */
    val determinizeHiddenInformation: Boolean = false,
    /**
     * Carry a candidate simulation through the combat damage step once blockers are declared,
     * instead of stopping at the empty stack that sits one step before it.
     *
     * The cheap half of what Phase 7's rollouts were bought for. Three of the suite's six
     * remaining failures pay mana now for a payoff that only lands at damage — a Fog, a
     * regeneration shield, a firebreathing pump — and a one-ply evaluator that stops at the empty
     * stack sees only the cost. Unlike a rollout this adds no sampling and no horizon *beyond*
     * combat, so it cannot manufacture the tempo blindness that made `v0-rollout` lose
     * `respond-02`.
     *
     * See [GameSimulator]'s constructor for why it starts at declare-blockers rather than
     * declare-attackers.
     */
    val resolveThroughCombatDamage: Boolean = false,
    /**
     * Charge an attack plan's estimated crack-back as the life it would actually cost, rather
     * than a flat −3.0 that only fires when the crack-back is exactly lethal.
     *
     * See [CombatAdvisor]'s constructor. The target is `race-03` — "keep the ground creature home
     * to block the 3/3" — which is the one attack-vs-hold position the evaluator owns rather than
     * `CombatSeed`, and which the rollouts do not close either.
     */
    val priceCrackBackAsLife: Boolean = false,
    /**
     * With [priceCrackBackAsLife], charge the life a crack-back would cost *without* chump blocks,
     * keeping chump blocks only for deciding whether the crack-back is lethal. See [CombatAdvisor]'s
     * constructor. Without it the life-priced crack-back holds a 2/2 home to chump a tapped 3/3
     * rather than attack past it — `race-06`, which [priceCrackBackAsLife] alone breaks.
     */
    val crackBackWithoutChumps: Boolean = false,
    /**
     * Stop charging a land drop as card loss. A land moving from hand to battlefield is not a card
     * spent, it is a card converted to mana — and mana is what [EvaluationWeights.tempo] and
     * `BoardPresence` already price.
     *
     * See [com.wingedsheep.ai.engine.evaluation.CardAdvantage]'s `heldCardCount`. The target is
     * `sequencing-02` — "on the last card in hand, still make the land drop" — the one puzzle every
     * profile measured so far fails, `production` and `production-candidate-tuned` included. It is
     * also the one whose real-game frequency is highest: any turn whose only card in hand is a land
     * is a turn the AI skips its land drop, and every game reaches several.
     *
     * Not the same lever as [EvaluationWeights.topdeckPenalty], which the promotion run reached for
     * and rejected. Moving the constant to −1.0 also closes this puzzle, but by making an empty hand
     * cheaper *everywhere* — which is why it cost `respond-02`. This changes what a hand *contains*
     * and leaves the cliff exactly where it is.
     */
    val landDropIsNotCardLoss: Boolean = false,
    /**
     * Judge a land drop by the mana it makes *usable*, not by whether the land arrives untapped.
     *
     * `BoardPresence` prices an untapped land 0.6 and a tapped one 0.3, flat — the only thing in the
     * evaluator that tells one land drop from another. It is the right ranking for the wrong reason,
     * and the reason matters on any turn where the extra mana is dead: with a 4-drop in hand and
     * three lands out, the tapland is *free* now and the basic is what you want untapped next turn,
     * so the constant plays the sequence exactly backwards and the 4-drop slips a turn.
     *
     * See [com.wingedsheep.ai.engine.evaluation.BoardPresence]'s `landSequencing`, which refunds the
     * charge when nothing in hand could have spent the mana and prices a tapland still *in* hand as
     * the deferred cost it is. Needs [useCardIntent]: "always enters tapped" is card knowledge, read
     * off [com.wingedsheep.ai.engine.knowledge.CardIntent.entersTapped].
     *
     * The targets are `sequencing-07` / `sequencing-08` — the same board, the same two lands, and
     * opposite right answers depending only on what the hand wants to cast. The flat constant can
     * solve one or the other, never both.
     */
    val sequenceLandsByUsableMana: Boolean = false,
    /**
     * A combat trick's window is the one where blocks are already in — not any step of combat.
     *
     * [com.wingedsheep.ai.engine.knowledge.HoldPolicy] pays a trick its combat bonus in every
     * combat step, `BEGIN_COMBAT` and `DECLARE_ATTACKERS` included. Both are *before* blocks, and
     * spending a trick there is worse than spending it late for a reason no board evaluation can
     * see: it hands the defender the information. The 2/2 that would have gone unblocked gets
     * chump-blocked once it is visibly a 5/5, and the pump that was exactly lethal buys nothing.
     *
     * The targets are `instants-07` / `instants-08` — the same board and the same trick one step
     * apart, cast it after no blocks, hold it before them. Needs [useCardIntent], which is what
     * `HoldPolicy` runs on at all.
     *
     * Pairs with [com.wingedsheep.ai.engine.budget.TieredBudgetPolicy.preDamageCombatIsNormal],
     * and the pair is not separable in either direction: this one alone teaches the AI to wait for
     * a window it then cannot spend, and that one alone teaches it to convert in a window it
     * should not have reached.
     */
    val combatTricksWaitForBlocks: Boolean = false,
    /**
     * Score the race in **urgency** — the share of a life total removed per turn — rather than in
     * turns, so a distant clock is discounted and an absent one is simply zero.
     *
     * [com.wingedsheep.ai.engine.evaluation.ThreatAssessment] measures the race by subtracting one
     * side's turns-to-kill from the other's, and hands a side with no attacker the sentinel `99.0`.
     * The sentinel goes into the subtraction: an empty board facing a lone 2/2 scores
     * `(99 − 10) × 1.5`, or −160 once weighted, on a scale where that same 2/2 is worth 3.6 of board
     * presence and a point of life is worth 1. It fires in both directions — a 5/5 against no
     * creatures reads **+190** — so every position with an empty board on one side is decided by
     * this one term.
     *
     * The sentinel is the symptom; measuring the race in raw turns is the cause. A turn is not a
     * turn: the gap between dying on turn 10 and turn 20 counted for as much as the gap between
     * dying next turn and the turn after, and a great deal happens in ten turns. Urgency is `1 /
     * turns`, so it discounts distance the way distance should be discounted, needs no sentinel, and
     * is linear in power — which turns got backwards, pricing 2 power → 4 at twice what it priced
     * 4 → 8. See [com.wingedsheep.ai.engine.evaluation.ThreatAssessment.RACE_URGENCY_SCALE] for the
     * scale sweep, and [PRODUCTION_RACECLOCK] for what it moves.
     *
     * The target is `lastchance-05` — Unsummon our own Serra Angel in response to a Murder, rather
     * than bouncing the opposing 2/2. Saving the Angel leaves their 2/2 on board and so pays the
     * −160; throwing it away clears the board and does not. Nothing about the *card* was misread,
     * and `PuzzleSuiteTest` was right to record the miss as target polarity: this is where the
     * polarity comes from.
     */
    val discountedRaceClock: Boolean = false,
    /**
     * Charge a removal spell for pointing at a creature that isn't worth a card yet — so the AI
     * stops spending its Pacifism on the first 1/1 across the table.
     *
     * A one-ply evaluator scores the board right after the removal resolves, sees an opposing
     * creature gone, and has no term at all for the option the card *was*. So the removal fires at
     * the first legal target, every game, whatever it is.
     *
     * Phase 6 built the obvious fix — a **constant** penalty on "instant removal in our own main
     * phase" — measured it, and removed it: the one large enough to change behaviour also vetoed
     * casting a Disenchant at the only artifact on the table. See
     * [com.wingedsheep.ai.engine.knowledge.HoldPolicy]'s KDoc for the verdict.
     *
     * [com.wingedsheep.ai.engine.knowledge.RemovalPatience] charges a different thing. Not the
     * window — the **target**, by how far its board value falls short of a creature the removal's
     * own mana value should expect to trade with, priced at the profile's `boardPresence` weight.
     * A 1/1 under a Murder is 2.8 points short and pays; a 3/3 is fair and pays nothing; a
     * Disenchant is not aimed at a creature at all and is out of scope by construction, which is
     * what makes this a proportional comparison rather than the preference a constant could not
     * price. It ends four ways: a **hard veto** on any turn where the opponent has lethal on board
     * (`ThreatAssessment.lethalOnBoardAgainst`, the same predicate the evaluator's own −10.0 term
     * uses — the AI must never sit on removal while it dies, and a magnitude argument is not a
     * guarantee); a hand at the discard limit; a bar that decays to nothing by turn 14; and, short
     * of lethal, a board score that outvotes it whenever the kill is genuinely urgent.
     *
     * The targets are `timing-01` — hold the Murder against a lone 2/2 with five open lands across
     * the table, the puzzle `KNOWN_FAILURES` records as "the exact case `HoldPolicy` declines on
     * purpose" — and the new `removal-07` / `removal-08` pair, which holds the board fixed and
     * moves only the hand size. Needs [useCardIntent], which is what the policy runs on at all.
     *
     * Note that `timing-01` can only move on a profile that *also* has [discountedRaceClock]: with
     * the turns-form sentinel in play, killing the opponent's only creature scores about +160, and
     * no defensible discount competes with that. [PRODUCTION_PATIENCE]'s attribution column is
     * therefore expected to be quiet there, and [PRODUCTION_CANDIDATE_PATIENCE]'s is not.
     */
    val holdRemovalForBetterTargets: Boolean = false,
    /**
     * The same question [holdRemovalForBetterTargets] asks about a creature, asked about a spell on
     * the stack: **is this worth the counterspell?**
     *
     * The target is `respond-02` — a Counterspell spent on a Grizzly Bears while the opponent still
     * holds cards and five untapped lands. The leaf sees an opposing spell gone and has no term for
     * the option the counter *was*, so it fires at whatever it is offered. On the live agent that
     * mistake beats passing by **+1.28**, where the four counters the same category says it *should*
     * make win by 10 to 20 — small enough for a discount to fix, and far enough from the right plays
     * that nothing correct is at risk.
     *
     * [com.wingedsheep.ai.engine.knowledge.CounterPatience] sets its bar at what the caster can still
     * deploy this turn — `1.4 × their untapped lands`, the same per-mana rate the removal bar uses —
     * and charges what the countered spell falls short of it. An opponent who tapped out scores a bar
     * of zero, which is why every other counterspell position in the suite is untouched by
     * construction; an instant or a sorcery is declined outright, because its worth *is* what it does
     * to the board and the leaf already simulates that. It shares
     * [com.wingedsheep.ai.engine.knowledge.Patience]'s three releases with the removal bar, plus one
     * of its own: an opponent with an empty hand has nothing better coming.
     *
     * The countered spell is priced by what it **is** rather than what it cost, which is what makes
     * an anthem come out right in both directions: "creatures you control get +1/+1" cast into an
     * empty board is worth letting resolve, and the same card cast by a player with ten creatures
     * prices out above any bar. Needs [useCardIntent], like everything else the policy reads.
     */
    val holdCountersForBetterSpells: Boolean = false,
    /**
     * Hand an instant-speed draw spell the opponent's-end-step window
     * [com.wingedsheep.ai.engine.knowledge.HoldPolicy] already hands instant-speed removal.
     *
     * The target is `timing-05` — two Islands, an Opt, the opponent's end step, and an AI that
     * passes to cleanup with the mana unspent. `KNOWN_FAILURES` has recorded the diagnosis since
     * Phase 2c: Phase 6 correctly switched off `Strategist`'s blanket `passScore - 1.5` at that
     * window, because the same constant also paid for dumping a pump that expires in cleanup, and
     * nothing replaced the half of it that was right. `HoldPolicy` only ever hands the end step back
     * to REMOVAL, so a DRAW-tagged instant gets nothing, and a cantrip's own board value is about
     * zero — a card drawn against a card spent — so it loses to passing at every window there is.
     *
     * Its pair is `instants-06`, which asserts the *opposite* about the same step: a Giant Growth
     * cast at the opponent's end step is thrown away. That stays answered without a second rule,
     * because an expiring pump is [com.wingedsheep.ai.engine.knowledge.IntentTag.COMBAT_TRICK] and
     * the trick branch runs first — which is the whole reason this is a window on a *tag* rather
     * than a discount on a *step*. Needs [useCardIntent], like everything else the policy reads.
     */
    val cashCantripsInTheEndStep: Boolean = false,
    /**
     * Stop deploying a **flash creature on our own turn** when the ambush window is still ahead.
     *
     * The target is `instants-09`, taken from a real game: turn 7, our own precombat main, a
     * Restoration Angel in hand and four Plains untapped, and the AI jams it. The leaf sees a 3/4
     * flier on the battlefield and scores it the same wherever in the turn it landed, so casting
     * beat passing by **+4.06** — the creature's whole board value — and every reason flash was
     * printed (hold the mana, see their attack, ambush a creature) is worth exactly zero to a
     * one-ply evaluator.
     *
     * [com.wingedsheep.ai.engine.knowledge.AmbushWindow] answers it with a
     * [com.wingedsheep.ai.engine.knowledge.TimingVerdict.NoWindow] rather than a discount, and its
     * KDoc carries the argument for why this is the one window branch that can honestly do that:
     * a bonus on the *good* window — the shape the removal branch uses — cannot correct a decision
     * made three steps earlier, and unlike "hold the removal" the claim is provable. Casting a
     * no-haste flash permanent now is dominated by casting it at the next free window *unless the
     * permanent does something in between*, and that list is finite and readable off the card:
     * it attacks, its ETB changes a combat, its ETB hands us a resource to spend, or there is
     * something on the stack. Any of the four and the floor declines.
     *
     * It inherits [com.wingedsheep.ai.engine.knowledge.Patience]'s three releases whole — lethal on
     * board, a hand at maximum size, a game past turn 14 — which is what keeps a floor this hard
     * from turning the card into a brick, and adds one of its own: once the opponent declares
     * attackers, holding longer buys nothing. Needs [useCardIntent], like everything else the
     * policy reads.
     *
     * Its negative controls are the rest of `instants` and the whole of `timing`: a flash creature
     * with haste, one whose ETB clears a blocker, and the ambush window itself, where it must
     * actually cast. A term that only ever says "don't" would score well on a suite by never
     * playing, which is why `instants-10` asserts the cast.
     */
    val holdFlashPermanentsForAmbush: Boolean = false,
    /**
     * Stop paying for an **activated ability whose whole payoff expires at cleanup** in a window
     * where nothing can spend it.
     *
     * The target is `instants-14`, taken from a real game: turn 4, the *opponent's* precombat main,
     * an Olivia's Dragoon (`Discard a card: This creature gains flying until end of turn`) that
     * entered last turn, and the AI discards a Battleground Geist to give it flying. A card, for a
     * keyword that is gone at cleanup, on a turn where the opponent controls no flier for it to
     * block and no combat it can attack in.
     *
     * The leaf scores it at **+2.35**, and both halves of that are the evaluator working as
     * designed. `BoardPresence.creatureBodyValue` prices flying at `1.5 + power × 0.3` = 2.1 (× the
     * 1.5 board weight = 3.15) with no reading of whether it is evasive against *this* board — the
     * same fact `ThreatAssessment.evasivePower` reads correctly two features over, where the
     * summoning-sick Dragoon contributes zero. And `CardAdvantage` charges the discarded card the
     * 4th-card marginal, 0.8. Neither number is defensibly wrong on its own; the position is.
     *
     * [com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow] answers it the way
     * [holdFlashPermanentsForAmbush] answers its own — a
     * [com.wingedsheep.ai.engine.knowledge.TimingVerdict.NoWindow] rather than a discount — and its
     * KDoc carries the argument. The short version is that the dominance claim is *stronger* here
     * than for a card in hand: an ability is not a resource that can be stripped, so it is provably
     * still available at `DECLARE_ATTACKERS`, at the same cost, with strictly more information.
     *
     * **This is also the first window verdict an activated ability has ever received.**
     * `HoldPolicy.verdictFor` resolves an activation to its source permanent's name, and
     * `CardIntentAnalyzer` types any permanent carrying a non-mana activated ability as
     * `Speed.ACTIVATED`, which `windowVerdictFor` declines at its first line. So the `COMBAT_TRICK`
     * branch — whose subject is exactly "a pump wears off at cleanup" — was unreachable for every
     * ability in the catalog, and the mistake `instants-06` pins for an instant went unmeasured for
     * the identical text printed on a creature.
     *
     * Three guards keep the floor honest, any one of which failing hands the decision back to the
     * leaf: every payoff in the effect tree is an until-end-of-turn grant, the ability is
     * instant-speed with a window still ahead, and nothing on the stack points at a permanent we
     * control. It inherits
     * [com.wingedsheep.ai.engine.knowledge.Patience]'s three releases on top — and the hand-size one
     * matters more here than anywhere else, because the commonest cost on this ability shape is a
     * discard, which is free at a full hand. Needs [useCardIntent], like everything else the policy
     * reads.
     */
    val holdExpiringGrantsForCombat: Boolean = false,
    /**
     * Aim a spell or ability at the side of the table its effect is *for*, derived from the effect
     * by [com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer] rather than from the one
     * shape the ranker used to recognise (a fixed stat boost on an activated ability).
     *
     * Three things change, all from the same reading:
     *
     *  1. **Ranking.** A beneficial slot — a damage shield, a pump spell, an evasion Aura — ranks
     *     our own best permanent first. Everything else used to rank as removal, which is how a
     *     Daru Healer spent its "prevent the next 1 damage to any target" on the opponent's
     *     Gustcloak Harrier (2026-10-09 AI-vs-AI log, game 18 turn 26): an opponent's permanent is
     *     always the best *removal* target, and the simulated board does not price a shield.
     *  2. **Wrong-side veto.** A candidate with a mandatory slot whose every legal target is on the
     *     wrong side is dropped, so the card is held: Blossombind on our own Explosive Prodigy
     *     because the opponent had no creatures (game 15 turn 12), Prohibit on our own Treefolk
     *     Healer — above its mana-value cap, so it did nothing at all — at 4 life (game 3 turn 21).
     *  3. **The unkicked cast stays a candidate.** `preferKickerVariants` dropped it whenever the
     *     kicked one was affordable, on the theory that a kicker only adds. A kicker adds an
     *     *effect*, whose target the cast cannot see: kicked Tolarian Emissary destroyed our own
     *     Traveler's Cloak, the only enchantment on the table (game 3 turn 19).
     *
     * Every slot the analyzer cannot read confidently is UNKNOWN and keeps the old behaviour.
     * Needs [useCardIntent]: the polarity is read off the card definition, which only the intent
     * catalog can reach.
     */
    val targetPolarityFromEffect: Boolean = false,
    /**
     * The two `BoardPresence.creatureValue` corrections [PRODUCTION_RACECLOCK]'s KDoc named as the
     * reason its arena win came with a puzzle trade — the damaged-creature discount and the flat
     * multiplier on "can't attack". Both are off by default; see
     * [com.wingedsheep.ai.engine.evaluation.CreatureValuation] for what each one is and why the old
     * number was wrong in shape rather than in size.
     *
     * Reaches only the composite evaluator, like every other flag here.
     */
    val creatureValuation: CreatureValuation = CreatureValuation.LEGACY,
    /**
     * Price the hand curve on **business** and lands separately and lower, instead of counting a
     * land as a card and then deleting one to make the land drop balance.
     *
     * The model in one line: *a land on the battlefield is worth more than a land in your hand, and
     * a land in your hand is still worth something.* That makes the land drop positive by
     * construction — the field side already pays 2.1 early and 1.14 late, the hand side gives up
     * [com.wingedsheep.ai.engine.evaluation.CardAdvantage.LAND_IN_HAND] — so it **supersedes**
     * [landDropIsNotCardLoss] rather than stacking with it. With this on, the earmark is not
     * consulted at all.
     *
     * What the earmark got wrong is not its arithmetic but which end it balanced at. It bought
     * neutrality by subtracting the land outright, so a hand of one Forest scored *exactly* what an
     * empty hand scored and 3.0 below a hand of one Grizzly Bears, and `[Forest, Bears]` scored what
     * `[Bears]` scored alone. The land contributed zero, always. An opponent handed the choice of
     * what to strip would take that land — a Duress priced at zero is the shortest proof the number
     * is wrong.
     *
     * The larger prize is **flood**, which the AI cannot currently see at all. Past the one earmarked
     * land the old model counts lands as full cards, so seven lands in hand score `cardValue(6)` =
     * 6.4 — a flooded hand reads as an excellent one. Here the same hand is `cardValue(0) + 7 × 0.5`
     * = 1.5. That is a large behavioural change on every hand containing a land, which is every
     * hand, and it is why this gets a real arena run rather than a puzzle column.
     */
    val priceLandsInHandAsMana: Boolean = false,
    /**
     * Rank every "which card do I keep / take / discard" answer — discard (including cleanup's
     * discard to hand size and connive), scry-to-bottom, "put one into your hand", library search
     * and reorder — by [CardSelectionValue]: a card's worth minus the turns until the player can
     * actually cast it, counting lands in hand, the unspent land drop, the lands left in the
     * library, and whether any source makes the colours it needs.
     *
     * Fixes four misplays from the 2026-10-09 AI-vs-AI log review, all in [DecisionResponder]'s
     * legacy `contextualCardScore`, which read only the number of lands on the battlefield:
     * Orazca Puzzle-Door taking a land over removal with a land already in hand (g11 T7); connive
     * pitching the only land with the drop unspent (g10 T11); a one-land hand discarding its
     * three-drop and keeping a seven-drop (g12 T5); and a hand with only Swamps discarding black
     * cards while keeping five white ones (g20 T11/T13). The untap-limit choice is a question about
     * permanents, not cards, and keeps the legacy ranking.
     */
    val castabilityAwareCardSelection: Boolean = false,
    /**
     * Answer the choices a one-step simulation can't tell apart with what the choice is *for*,
     * instead of with the first option. Three misplays from the 2026-10-09 AI-vs-AI log review:
     *
     * - **Colour and land-type ties** go to the colour that matters. A mana source's choice leans to
     *   its controller's unpaid pips (game 4: Room of Refuge chose white in an R/G deck and tapped
     *   for dead mana for the rest of the game); any other — landwalk, protection, a hoser — leans
     *   to what the opponents have (game 3: Traveler's Cloak granted islandwalk against Forests and
     *   Swamps).
     * - **Card names and creature types** are shortlisted from the cards actually in the game and
     *   only those are simulated, instead of every name in the registry. Ties lean to the
     *   opponents' cards for names (game 9: Lammastide Weave named "A Killer Among Us", the
     *   alphabetically first card, against a Lorwyn deck) and to our own for creature types.
     * - **A pending "skip your draw step" marker** is priced as the card it costs (game 7: Fasting
     *   skipped five draws on two lands, because the choice is made in the upkeep and the simulation
     *   stops before the draw it gives up).
     *
     * Every rule is a tie-break or a missing price, never an override: an option the simulation
     * already scores higher still wins. See [ChoicePriors] for the weights and the information
     * they read — the multiset of each player's hand and library, which is no more than the
     * search's own determinizer assumes.
     */
    val informedChoiceDecisions: Boolean = false,
    /**
     * Choose *how many* cards a selection takes by what they are worth, not by the decision's
     * bounds. Three misplays from the 2026-10-10 engine-vs-engine logs (`production-candidate-expiring`),
     * all in [DecisionResponder]'s generic card-selection fallbacks:
     *
     * - **A search with no more candidates than it may take found nothing.** The pipeline's library
     *   search arrives as an unlabelled "choose up to N", and the fallback took `min` (zero)
     *   whenever `max` was not smaller than the option count. Game 3 turns 19/22/25: a Landscape
     *   sacrificed with [Plains] as the only option, three times, leaving the AI on four lands
     *   with a seven-drop in hand.
     * - **Scry always bottomed a card.** The bottom pile reused the discard path's
     *   `coerceAtLeast(1)`: game 1 turn 12, Lembas bottomed Celeborn the Wise with six lands out
     *   and only lands in hand.
     * - **Surveil never milled.** "Put in graveyard" matched no branch and took zero: Hidden Grotto
     *   (game 1) and Refute Destiny (game 2) kept every card on top.
     *
     * With it on, an unlabelled "choose up to N" takes N, and scry / surveil send away exactly the
     * cards whose [CardSelectionValue] falls below a keep threshold — zero, some or all.
     */
    val selectionCountsByValue: Boolean = false,
    /**
     * Refuse to pay for an until-end-of-turn payoff that nothing can spend this turn: an activation
     * or a combat trick whose every effect wears off at cleanup, in a window where the creature it
     * buys for is not in a combat that is still ahead of it.
     *
     * The leaf score reads an end-of-turn grant as permanent — `BoardPresence` prices deathtouch,
     * +1/+1 or a new land type on the board as it stands, and the board as it stands is before
     * cleanup. So it pays for the grant wherever it is bought, and off a 24-game log review it was
     * bought wherever the AI had spare mana: Esquire of the King's team pump in our precombat main on
     * six turns that declared no attack, Poison Dart Frog's deathtouch on turns the Frog never
     * attacked (four activations in one main phase, two of them on a Frog already granted it), Dream
     * Thrush turning an opponent's tapped land into a Plains on our own turn and staying tapped
     * through their attack at 4 life, Acrobatic Leap cast on a Market Gnome that was not in the
     * combat it was cast in. See [com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow] for the
     * activation half and [com.wingedsheep.ai.engine.knowledge.HoldPolicy] for the trick half.
     *
     * With [holdExpiringGrantsForCombat] also on, two things change about that flag's deferral
     * floor: it reads the same wider set of shapes (a group pump, a land-type change, an ability
     * that taps its own source as a drawback), and it stops inheriting `Patience`'s long-game
     * release. That release is a bet that a *better card* is coming; the deferral is a claim about
     * a window later *this turn*, which a long game does not weaken — and it was the release that
     * let the Frog's deathtouch through from turn 14 on. Needs [useCardIntent].
     */
    val refuseUnspendableGrants: Boolean = false,
    /**
     * Blocking local search may not add a chump block — a blocker that dies while everything it
     * blocked survives — unless the life it saves is life we need: this hit is lethal, or it leaves
     * us dead to the attack after (`CombatAdvisor.isLifeInDanger`, counting every creature that
     * will have untapped by then rather than only today's untapped ones).
     *
     * Fixes the high-life chump blocks in the 2026-10-09 AI-vs-AI logs: a 2/3 in front of a 5/5
     * first-striking trampler at 18 life (it saved three points and lost the creature), a 0/3 wall
     * in front of a 3/3 at 18, a 1/1 in front of a 2/2 at 12, a 3/1 in front of a 6/4 at 20 with
     * the opponent at 8, a 1/1 in front of a 2/3 at 25. None came from the chump passes — every
     * one was a local-search "add a blocker" mutation the one-ply evaluator scored a fraction of a
     * point higher than taking the hit. The evaluator prices a creature by what it is worth on the
     * board after this combat; it cannot see that a creature alive next turn blocks again, so it
     * sells bodies for life it does not need. The chump passes already encode the policy ("chump
     * only when facing lethal"); this stops the search from routing around it.
     *
     * A losing block the heuristic seed already chose is left alone — the gate is on what search
     * *adds*. Read off the simulated combat, not predicted, so first strike and deathtouch count.
     */
    val chumpOnlyWhenInDanger: Boolean = false,
    /**
     * Stop charging a permanent cast at sorcery speed as card loss — [landDropIsNotCardLoss]'s
     * argument, one card type further.
     *
     * Casting a creature in our own main phase does not spend a card, it moves one from the zone
     * `CardAdvantage` prices to the one `BoardPresence` prices. Charged as card loss, the move only
     * happens when the permanent's board value beats what the hand curve says the card was worth
     * *held* — and at the empty-hand cliff that is 3.0 (1.0 → `concave-hand-2`'s −2.0), more than a
     * summoning-sick 1/1 or a mana rock is worth on any board. So the AI sat on its last creature for
     * turns with ten lands untapped: Wose Pathfinder for five turns while six attackers killed it
     * (2026-10-09 logs, game 21 T19–T23), Hecteyes at 2 life with its only spare blocker in hand
     * (game 23 T21), Changeling Wayfinder with nine lands (game 15 T22/T24). The same charge one rung
     * up — 1.5 for the second card — is what kept a 1/1 pinger in hand next to two other cards
     * (game 13 T14) and a Dragonstorm Globe uncast for eight turns (game 9).
     *
     * The hand curve is the right price for a card whose value is the *option* of casting it: an
     * instant waiting for its window, a counterspell, a trick. A sorcery-speed permanent with the
     * mana to cast it has no window to wait for — passing just moves it to a later main phase with
     * the same mana and less game left, and mana unspent at end of turn is worth nothing. So
     * `Strategist` refunds exactly what the evaluator charged for taking that card out of hand
     * (`evaluate(root) − evaluate(root without the card)`), and only for a cast that put the card
     * onto our battlefield from our own main phase with the stack empty. The board side then decides
     * alone, the way [landDropIsNotCardLoss] lets `Tempo` decide the land drop. Instants, sorceries,
     * flash plays on the opponent's turn and an Adventure's spell half are untouched — their hand
     * value is real option value.
     */
    val permanentCastIsNotCardLoss: Boolean = false,
    /**
     * Score `ThreatAssessment`'s evasion term the way it reads after an attack, so attacking with a
     * flier the opponent cannot block stops reading as a loss.
     *
     * An attack plan is scored on the post-combat state, and two things in the evasive-power term
     * read that state wrong:
     * - **It dropped every tapped attacker.** Everything that just attacked is tapped, so sending a
     *   flier the opponent cannot block erased exactly the evasion that made the attack free.
     *   `ThreatAssessment.attackPotential` already refuses to filter by tapped for this reason; this
     *   applies its argument to the evasion term. The defenders' tapped fliers still do not count —
     *   a tapped creature genuinely cannot block.
     * - **It counted a flier that can't block as air cover.** Aesthir Glider at home "blocked" the
     *   opponent's fliers, so attacking with it read as opening the skies.
     *
     * At 0.5 a point under the 1.2 threat weight the first costs 0.6 per point of power, more than
     * the 0.5 a point of opponent life above 15 is worth, so the AI sat on its fliers: three to four
     * of them into a board with no flier or reach for three turns running (AI game-log review
     * 2026-10-09, game 21 turns 12–16), and a can't-block Aesthir Glider held home while the
     * opponent's only flier was tapped (game 22 turn 13).
     *
     * Reaches only the composite evaluator, like every other evaluator flag here.
     */
    val evasionAfterAttacking: Boolean = false,
    /**
     * Count the land drop still to come as mana when [sequenceLandsByUsableMana] decides whether
     * tapped lands were idle — so playing an untapped land can never score below passing.
     *
     * The idle-mana refund asks whether anything in hand would become castable if the tapped lands
     * untapped, and it read the answer off the battlefield alone. Playing a land slid that window up
     * by one: tapped out on four lands holding only 5-drops, the four tapped lands were refunded as
     * idle (+1.2); play the fifth and the 5-drops became "castable but for the tapped lands", the
     * refund vanished, and the land drop lost to passing. AI game-log review 2026-10-10 (profile
     * `production-candidate-expiring`), game 3 turn 11: after tapping out for Aerie Auxiliary the AI
     * passed both main phases with "Play Shattered Landscape" on offer. Rollouts cannot break the
     * tie, because the playout policy plays the land on both branches.
     *
     * See [com.wingedsheep.ai.engine.evaluation.BoardPresence]'s `landSequencing`. Only the states
     * *before* a land drop move; every post-drop state, and so every tapland-vs-basic comparison,
     * scores as before. Needs [sequenceLandsByUsableMana]; reaches only the composite evaluator.
     */
    val pendingLandDropIsMana: Boolean = false,
    /**
     * Answer an "up to N targets" decision with as many targets as help, not always exactly one.
     *
     * `DecisionResponder` simulated each legal target alone and returned the best single one, so
     * every multi-target requirement got one target whatever its maximum: Aerie Auxiliary's
     * "support 2" put its counter on one creature with a second friendly one standing beside it
     * (2026-10-10 AI-vs-AI logs, game 3 turns 11 and 25), and a mandatory "two target creatures"
     * trigger was answered with an illegal single target. With this on, the answer grows greedily
     * from the best single pick: each round adds the remaining candidate whose simulated result is
     * best, and stops when nothing improves on the answer so far or the maximum is reached — so a
     * counter is never handed to the opponent's creature just because a slot was free. Below the
     * requirement's minimum a legal pick is added whatever it scores.
     *
     * The decision half only. `Strategist` fills the targets of a spell or ability it casts the
     * same way, which this leaves alone.
     */
    val fillUpToMaxTargets: Boolean = false,
    /**
     * Refuse an activated ability whose whole payoff is a search of our own library when nothing in
     * that library matches — a fetch land with its basics gone.
     *
     * 2026-10-10 AI-vs-AI logs, game 3 turn 17: Seething Landscape (search for a basic Island, Swamp
     * or Mountain) was cracked in response to Spell Stutter from a Forest/Plains deck, losing a land
     * for nothing. The leaf score was against it; the likeliest reason it won anyway is rollout
     * noise, since the activation's shuffle reorders the library and unpairs its playouts from the
     * pass line's. With this on, `HoldPolicy` floors such an activation below
     * passing. Only the whole-payoff shape is refused: tap/mana/life/sacrifice-self costs, a search
     * pipeline and nothing else, from a non-creature source. See
     * [com.wingedsheep.ai.engine.knowledge.DeadSearch]. Needs [useCardIntent].
     */
    val refuseDeadSearches: Boolean = false,
    /** Non-null profiles may only be selected automatically for this set. Arena selection stays explicit. */
    val restrictedToSet: String? = null,
) {
    companion object {
        /**
         * The frozen reference opponent: greedy 1-ply, default weights, no card advisors.
         * Pinned as of Phase 1 (2026-07-27). **Do not change this.**
         */
        val LEGACY_V0 = AiProfile(id = "v0")

        /**
         * What `AIPlayer.create(registry, playerId)` builds today. Identical to [LEGACY_V0] right
         * now — it diverges as later phases turn features on, and the gap between the two is
         * exactly what the arena measures.
         */
        val CURRENT = AiProfile(id = "current")

        /**
         * What a player faced in a real game **up to 2026-08-07**, when
         * [PRODUCTION_CANDIDATE_TUNED] replaced it in [EngineAiPlayerController].
         *
         * Kept, unchanged, as the baseline every promotion is measured against — the same job
         * [LEGACY_V0] does for the plan's phases, one level up. A candidate has to beat *this*,
         * not `v0`, because `v0` carries neither the card advisors nor `CardIntent` that shipped
         * long ago. It is still what `PuzzleSuiteTest` runs, so `KNOWN_FAILURES` keeps describing
         * a fixed agent rather than drifting with whatever is live.
         */
        val PRODUCTION = AiProfile(
            id = "production",
            advisorModules = listOf(BloomburrowAdvisorModule(), OnslaughtAdvisorModule()),
            useCardIntent = true,
        )

        /**
         * What [PRODUCTION] would become if Phases 4, 7 and 8 were switched on for real players:
         * the shipped card advisors and `CardIntent`, plus the meaningful-action filter, the
         * four-tier budget, the rollout evaluator and fair play. The arena agent
         * `production-candidate`.
         *
         * **This is the only profile that answers the promotion question**, and until it existed
         * nothing did. Every number the plan publishes is quoted against [LEGACY_V0], which carries
         * neither advisors nor `CardIntent` — so "the rollouts are worth +6%" is a statement about
         * an agent nobody plays against. The gate is `just arena production production-candidate`;
         * the compounding check is `just arena v0 production-candidate`.
         *
         * [TieredBudgetPolicy] is not optional here, unlike in the phase-isolating profiles.
         * [LegacyBudgetPolicy] has no global deadline, so rollouts under it are a search a real
         * player waits on with nothing to stop it — acceptable in an arena that spends its budget
         * as a simulation count, not in a session with a human on the other end.
         */
        val PRODUCTION_CANDIDATE = PRODUCTION.copy(
            id = "production-candidate",
            useMeaningfulFilter = true,
            budgetPolicy = TieredBudgetPolicy(),
            rollouts = RolloutSettings.DEFAULT,
            determinizeHiddenInformation = true,
        )

        /**
         * The two targeted fixes for the suite's remaining failures, without rollouts:
         * the combat-damage horizon and the concave hand curve, on top of what already ships.
         *
         * Between them they aim at four of `PuzzleSuiteTest.KNOWN_FAILURES` — `instants-05` and
         * `activate-05` from the horizon, `sequencing-02` and `noncreature-02` from the curve —
         * and they cost essentially nothing: one extra step of simulation inside combat, and a
         * different constant. That makes this the control that says whether
         * [PRODUCTION_CANDIDATE]'s rollouts are still paying for themselves once the cheap fixes
         * are in, rather than being credited for what a constant would have bought.
         */
        val PRODUCTION_TUNED = PRODUCTION.copy(
            id = "production-tuned",
            useMeaningfulFilter = true,
            budgetPolicy = TieredBudgetPolicy(),
            evalWeightsId = "concave-hand",
            resolveThroughCombatDamage = true,
        )

        /**
         * Attribution controls for [PRODUCTION_TUNED], which moves four things at once. Each of
         * these moves exactly one, so a puzzle that changes can be blamed on something.
         */
        val PRODUCTION_HORIZON = PRODUCTION.copy(
            id = "production-horizon",
            resolveThroughCombatDamage = true,
        )
        val PRODUCTION_CONCAVE = PRODUCTION.copy(
            id = "production-concave",
            evalWeightsId = "concave-hand",
        )

        /** The same curve fix at half strength: empty hand at −2.0 rather than −1.0. */
        val PRODUCTION_CONCAVE_2 = PRODUCTION.copy(
            id = "production-concave-2",
            evalWeightsId = "concave-hand-2",
        )

        /** The crack-back pricing on its own, so `race-03` moving can be attributed to it. */
        val PRODUCTION_CRACKBACK = PRODUCTION.copy(
            id = "production-crackback",
            priceCrackBackAsLife = true,
        )

        /** The land-drop accounting on its own, so `sequencing-02` moving can be attributed to it. */
        val PRODUCTION_LANDDROP = PRODUCTION.copy(
            id = "production-landdrop",
            landDropIsNotCardLoss = true,
        )

        /** All three targeted fixes. The best the suite can be pushed to without curve-fitting. */
        val PRODUCTION_TARGETED = PRODUCTION.copy(
            id = "production-targeted",
            evalWeightsId = "concave-hand-2",
            resolveThroughCombatDamage = true,
            priceCrackBackAsLife = true,
        )

        /** Horizon plus each curve value, to pick the pair that costs nothing. */
        val PRODUCTION_HORIZON_CONCAVE = PRODUCTION.copy(
            id = "production-horizon-concave",
            evalWeightsId = "concave-hand",
            resolveThroughCombatDamage = true,
        )
        val PRODUCTION_HORIZON_CONCAVE_2 = PRODUCTION.copy(
            id = "production-horizon-concave-2",
            evalWeightsId = "concave-hand-2",
            resolveThroughCombatDamage = true,
        )

        /**
         * What a player faced in a real game **from 2026-08-07 until 2026-08-08**, when
         * [PRODUCTION_CANDIDATE_LANDDROP] replaced it in [EngineAiPlayerController]. Kept unchanged
         * as the baseline that promotion was measured against — the same job [PRODUCTION] does for
         * this one.
         *
         * [PRODUCTION_CANDIDATE] with the two cheap fixes on top. The two scoreboards turned out
         * to measure nearly orthogonal things, which is why this is a combination rather than a
         * choice. The rollouts win the arena (**+7.3%** against `production` over 300 paired
         * games) and *cost* four puzzles; the combat-damage horizon and the concave hand curve win
         * three puzzles and are arena-neutral (49.7%, CI [48.7%, 50.7%]). Neither result argues
         * against the other, so this takes both — measured together at **+6.7%**
         * (`production` 43.3%, CI [39.3%, 47.0%]) and 60/66 on the suite, level with `production`.
         *
         * `concave-hand-2` rather than `concave-hand`: −1.0 also closes `sequencing-02`, but it
         * starts spending the last Counterspell on a 2/2 with seven lands open, and `respond-02`
         * is the negative control that exists to catch exactly that.
         *
         * The id stays `production-candidate-tuned` after promotion on purpose — every arena
         * report and CSV row already published under that name refers to this exact agent, and
         * renaming it to match its new status would break that.
         */
        val PRODUCTION_CANDIDATE_TUNED = PRODUCTION_CANDIDATE.copy(
            id = "production-candidate-tuned",
            evalWeightsId = "concave-hand-2",
            resolveThroughCombatDamage = true,
        )

        /**
         * What a player faced in a real game **from 2026-08-08 until 2026-08-09**, when
         * [PRODUCTION_CANDIDATE_LANDSEQ] replaced it in [EngineAiPlayerController]. Kept unchanged
         * as the baseline that promotion was measured against.
         *
         * [PRODUCTION_CANDIDATE_TUNED] plus [landDropIsNotCardLoss] — the agent that finally makes
         * its land drop. Promoted on the same bar the two fixes above it were: **arena-neutral and
         * a puzzle ahead**. 300 paired games against `production-candidate-tuned` measured 49.0%,
         * CI [46.3%, 51.3%], 300/300 completed, 0 illegal actions; the accounting on its own
         * (`production` vs `production-landdrop`, 400 games) 49.5%, CI [46.8%, 52.3%]. On the
         * 66-puzzle suite it closes `sequencing-02` — the only puzzle every profile before it
         * failed — and moves no other verdict, for the live pair and for `production` alike.
         *
         * A new id rather than a flag flipped on the live profile, for the reason
         * [PRODUCTION_CANDIDATE_TUNED]'s own KDoc gives: every arena report already published under
         * that name refers to that exact agent, and the baseline a promotion is measured against
         * cannot be the thing being promoted.
         */
        val PRODUCTION_CANDIDATE_LANDDROP = PRODUCTION_CANDIDATE_TUNED.copy(
            id = "production-candidate-landdrop",
            landDropIsNotCardLoss = true,
        )

        /**
         * [sequenceLandsByUsableMana] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it and nothing else — the same isolation
         * [PRODUCTION_LANDDROP] gives the land-drop accounting.
         */
        val PRODUCTION_LANDSEQ = PRODUCTION.copy(
            id = "production-landseq",
            sequenceLandsByUsableMana = true,
        )

        /**
         * What a player faced in a real game **on 2026-08-09**, until
         * [PRODUCTION_CANDIDATE_TRICKWINDOW] replaced it in [EngineAiPlayerController]. Kept
         * unchanged as the baseline that promotion was measured against.
         *
         * [PRODUCTION_CANDIDATE_LANDDROP] plus [sequenceLandsByUsableMana] — the agent that plays its
         * lands in the right order. Promoted on the same bar the three fixes above it were,
         * **arena-neutral and a puzzle ahead** — but on *one* of the two arena runs that bar usually
         * gets, so read the next paragraph before quoting it. The term isolated on `production`
         * (`just arena production production-landseq 300`) measured 49.7%, CI [46.7%, 53.0%],
         * 149W-151L, 300/300 completed, 0 illegal actions: parity, which is the pass condition.
         * `production-candidate-landdrop` vs `production-candidate-landseq` — the same measurement
         * with rollouts on both seats — was still running when this was promoted. **If it comes back
         * below parity, revert the two call sites** ([EngineAiPlayerController] and
         * [AiProfileSelector]'s fallback) rather than the term: the flag is off for every other
         * profile, so backing the promotion out costs nothing and loses no measurement.
         *
         * On the 80-puzzle suite it closes `sequencing-07` and moves no other verdict, taking
         * `sequencing` to 8/8. Isolated on top of `production` (`production-landseq`) the same term
         * closes `sequencing-07` *and* `noncreature-02` — the Disenchant that missed by 0.40 — for
         * the reason its own KDoc predicted it could not be fixed by tuning: refunding idle mana
         * removes a standing charge against tapping out, rather than inflating the anthem prior.
         * That column measured 49.7%, CI [46.7%, 53.0%], 300/300 completed, 0 illegal actions.
         */
        val PRODUCTION_CANDIDATE_LANDSEQ = PRODUCTION_CANDIDATE_LANDDROP.copy(
            id = "production-candidate-landseq",
            sequenceLandsByUsableMana = true,
        )

        /**
         * [combatTricksWaitForBlocks] alone on top of [PRODUCTION], so a puzzle that moves is
         * attributable to it — the same isolation [PRODUCTION_LANDSEQ] gives land sequencing.
         *
         * Only *half* the pair is expressible here, and that is not an oversight:
         * [PRODUCTION] runs on [LegacyBudgetPolicy], which has no tiers, so
         * [com.wingedsheep.ai.engine.budget.TieredBudgetPolicy.preDamageCombatIsNormal] is a
         * no-op on it and its own attribution column would be empty. The budget half only exists on
         * an agent that tiers its search, which is why its isolation is
         * [PRODUCTION_CANDIDATE_TRICKWINDOW] rather than a `production-*` twin.
         */
        val PRODUCTION_TRICKWINDOW = PRODUCTION.copy(
            id = "production-trickwindow",
            combatTricksWaitForBlocks = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_LANDSEQ] plus both halves of the combat
         * trick window — hold the trick until blocks are in, and give the pre-damage window enough
         * budget to spend it.
         *
         * The two flags are one change and must be measured as one. Measured on the same board (an
         * unblocked 2/2, Giant Growth, opponent at 5) `production-candidate-landseq` **passes** —
         * the window is graded `ROUTINE`, and at 200 ms that drops the simulation-refined target
         * pick that would have found the kill. Fixing that alone makes the same agent cast the
         * trick in `DECLARE_ATTACKERS` instead, which is the telegraph [combatTricksWaitForBlocks]
         * exists to stop. Either flag on its own trades one mistake for the other.
         *
         * On the 83-puzzle suite it takes the live pair from **73/83 to 76/83**, and the three it
         * closes are every combat-trick conversion the agent was missing — `instants-02` (pump the
         * blocked attacker), `instants-03` (pump the blocker) and the new `instants-07` (pump for
         * exact lethal). Its failing set is a strict subset of `production-candidate-landseq`'s, so
         * nothing was traded for them. Both come from the budget half: two of the three predate
         * this work and were never diagnosed, because the missing search looks exactly like a
         * missing evaluation until you vary the tier.
         *
         * What a player faced in a real game until [PRODUCTION_CANDIDATE_RACECLOCK] replaced it in
         * [EngineAiPlayerController]. Kept unchanged as the baseline that promotion was measured
         * against. (No end date here on purpose — the dates on the four entries above it already run
         * ahead of the commit clock, and adding a fifth would make the sequence contradict itself.)
         *
         * Promoted on the usual bar, **arena-neutral and a puzzle ahead**, but read the sample size
         * before quoting it. `just arena production-candidate-landseq production-candidate-trickwindow
         * 100` measured the baseline at **47.0%, CI [43.0%, 50.0%]**, 47W-53L, 100/100 completed —
         * so the interval spans parity (at its upper edge) and the point estimate favours this
         * agent. That is a pass, and it is **100 games, not the 300** the three promotions above it
         * were held to: enough to rule out a large regression, not enough to resolve a small one.
         * The puzzle side is where the evidence is, at +3 with nothing traded.
         *
         * If a later run comes back below parity, revert the two call sites
         * ([EngineAiPlayerController] and [AiProfileSelector]'s fallback) rather than the flags:
         * both are off for every other profile, so backing the promotion out costs nothing and
         * loses no measurement.
         */
        val PRODUCTION_CANDIDATE_TRICKWINDOW = PRODUCTION_CANDIDATE_LANDSEQ.copy(
            id = "production-candidate-trickwindow",
            combatTricksWaitForBlocks = true,
            // A fresh policy rather than a derived one, because `budgetPolicy` is typed as the
            // interface and there is nothing to copy from. Sound only while the inherited policy is
            // `TieredBudgetPolicy()` at its default size — which it is, on [PRODUCTION_CANDIDATE];
            // if that ever takes a `normalMillis`, this line has to carry it too or the promotion
            // run silently measures two changes.
            budgetPolicy = TieredBudgetPolicy(preDamageCombatIsNormal = true),
        )

        /**
         * [discountedRaceClock] alone on top of [PRODUCTION], so a puzzle or an arena point that
         * moves is attributable to it — the same isolation [PRODUCTION_LANDSEQ] gives land
         * sequencing.
         *
         * **The arena says this is a real strength gain, and the puzzle suite says it costs
         * nothing.** `just arena production production-raceclock 300` measured `production` at
         * **43.7%, CI [40.0%, 47.7%]**, 131W-169L, 300/300 completed, 0 illegal actions — the whole
         * interval below parity, which is the first time one of these evaluator fixes has moved the
         * arena at all rather than merely failing to break it. On the 83-puzzle suite it is level at
         * 71/83, closing `lastchance-05`, `race-03` and `timing-01` and losing `activate-04`,
         * `instants-03` and `removal-03`.
         *
         * That trade is the second finding, and it is about `BoardPresence` rather than about this
         * term. All three losses are puzzles that pass today *because* the sentinel is out of scale,
         * standing in for board-quality the evaluator gets wrong:
         *  - `activate-04` sends the ping at the opponent's face rather than at a 3/3 only because a
         *    1-power board makes one point of face damage worth a whole turn of clock. Behind it,
         *    `BoardPresence.creatureValue` discounts a *damaged* creature by up to half — though
         *    marked damage wears off at cleanup, which is the exact case the
         *    `temporaryPTModification` discount five lines above it was written to avoid.
         *  - `removal-03` kills the Hill Giant rather than the Pacifism'd Craw Wurm only because the
         *    pacified creature contributes no `attackPotential`, so killing the other one sends the
         *    opponent to the 99-turn sentinel. Behind it, a creature that cannot attack at all is
         *    discounted by ×0.85.
         *
         * Fix those two and the trade should become a straight gain. Neither belongs in this change:
         * each is its own flag, its own attribution column and its own arena run.
         */
        val PRODUCTION_RACECLOCK = PRODUCTION.copy(
            id = "production-raceclock",
            discountedRaceClock = true,
        )

        /**
         * [PRODUCTION_CANDIDATE_TRICKWINDOW] plus [discountedRaceClock] — the agent that stops
         * pricing an empty board as a 99-turn clock.
         *
         * What a player faced in a real game until [PRODUCTION_CANDIDATE_PATIENCE] replaced it in
         * [EngineAiPlayerController]. Kept unchanged as the baseline that promotion is measured
         * against.
         *
         * The first promotion here to clear the bar on the *arena* half rather than on the puzzle
         * half. `just arena production-candidate-trickwindow production-candidate-raceclock 300`
         * measured the baseline at **45.3%, CI [41.3%, 49.3%]**, 136W-164L, 300/300 completed, 0
         * illegal actions — the whole interval below parity, with rollouts on both seats. The term
         * isolated on `production` (`just arena production production-raceclock 300`) says the same
         * thing more strongly: **43.7%, CI [40.0%, 47.7%]**, 131W-169L. Two independent 300-game
         * runs, neither CI touching parity.
         *
         * On the suite it is level at 76/83, closing `lastchance-05` and `race-03` and losing
         * `activate-04` and `removal-03` — the two `BoardPresence` weaknesses [PRODUCTION_RACECLOCK]
         * itemizes, which the old term's sentinel had been masking. Level is normally *below* the
         * bar; it is a pass here because the arena, which is the scoreboard that matters and the one
         * these fixes usually cannot move, is unambiguous in both columns.
         *
         * If a later run comes back below parity, revert the two call sites
         * ([EngineAiPlayerController] and [AiProfileSelector]'s fallback) rather than the flag: it
         * is off for every other profile, so backing the promotion out costs nothing and loses no
         * measurement.
         */
        val PRODUCTION_CANDIDATE_RACECLOCK = PRODUCTION_CANDIDATE_TRICKWINDOW.copy(
            id = "production-candidate-raceclock",
            discountedRaceClock = true,
        )

        /**
         * [holdRemovalForBetterTargets] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it — the same isolation [PRODUCTION_LANDSEQ] gives land
         * sequencing.
         *
         * Read this column for what patience *costs*, not for what it closes. `timing-01`, the
         * puzzle the term exists for, cannot move here: `PRODUCTION` scores the race in turns, and
         * the `99.0` no-attacker sentinel prices killing the opponent's only creature at about +160
         * — see [PRODUCTION_RACECLOCK] for the arithmetic. What this column *can* say is whether
         * holding removal breaks any of the 86 positions that have nothing to do with it, and the
         * answer is no: **73/86, failing set identical to `production`'s.** A term that moves
         * nothing on the agent it cannot help is the cheapest evidence available that it is not
         * quietly taxing every removal spell in the suite.
         */
        val PRODUCTION_PATIENCE = PRODUCTION.copy(
            id = "production-patience",
            holdRemovalForBetterTargets = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_RACECLOCK] plus [holdRemovalForBetterTargets]
         * — the agent that stops spending its removal on the first small creature it sees.
         *
         * On the 86-puzzle suite it takes the live pair from **78/86 to 80/86**, closing both
         * puzzles the term names — `removal-07` (Murder on a 1/1 with slack in hand) and
         * `timing-01`, which `PuzzleSuiteTest.KNOWN_FAILURES` has recorded since Phase 2c as "the
         * exact case `HoldPolicy` declines on purpose". Its failing set is a **strict subset** of
         * `production-candidate-raceclock`'s, so nothing was traded for them; `removal-08` and
         * `removal-09` — the same board with a full hand and on turn twenty — stay passing, which
         * is what says the AI is weighing a trade rather than refusing to spend removal.
         *
         * What a player faced in a real game until [PRODUCTION_CANDIDATE_BOARDVALUE] replaced it in
         * [EngineAiPlayerController]. Kept unchanged as the baseline that promotion was measured
         * against.
         *
         * **Promoted on the puzzle half; the arena half came back null rather than neutral.**
         * `just arena production-candidate-raceclock production-candidate-patience 100` measured
         * **50.0%, CI [50.0%, 50.0%]**, 50W-50L, 100/100 completed, 0 illegal actions — and all 50
         * pairs at 1-1-0, where the three runs before this one had 10, 36 and 37 decisive pairs out
         * of 150. A zero-width CI is a fact about the measurement before it is one about the agent:
         * this says the flag changes the outcome of a real sealed game *rarely*, not that it is
         * worthless and not that it is safe.
         *
         * The isolation column says the same thing (`production` vs `production-patience`, 100
         * games, 30 s: 50.0%, 0 decisive pairs), so it was measured directly rather than argued
         * about: instrumenting `discount()` over 20 games puts the fire rate at **about once per
         * game** — 19 non-zero discounts in 463 turns. Real, and far smaller than 50 pairs can
         * resolve. Notably the three releases are *not* what makes it rare (13 stops against 27
         * that reached the bar), so lengthening the patience window is not the lever if this is
         * ever tuned for more effect. Full numbers, the pair-distribution table and a harness
         * caveat that cost two discarded measurements: `docs/ai/baseline-metrics.md`.
         *
         * If a later run comes back below parity, revert the two call sites
         * ([EngineAiPlayerController] and [AiProfileSelector]'s fallback) rather than the flag: it
         * is off for every other profile, so backing the promotion out costs nothing and loses no
         * measurement.
         */
        val PRODUCTION_CANDIDATE_PATIENCE = PRODUCTION_CANDIDATE_RACECLOCK.copy(
            id = "production-candidate-patience",
            holdRemovalForBetterTargets = true,
        )

        /**
         * [CreatureValuation.markedDamageFadesAtCleanup] alone on top of [PRODUCTION], so a puzzle
         * or an arena point that moves is attributable to it — the same isolation
         * [PRODUCTION_LANDSEQ] gives land sequencing.
         */
        val PRODUCTION_DAMAGEFADES = PRODUCTION.copy(
            id = "production-damagefades",
            creatureValuation = CreatureValuation(markedDamageFadesAtCleanup = true),
        )

        /**
         * [CreatureValuation.cantAttackCostsPower] alone on top of [PRODUCTION], same isolation.
         *
         * Expected to be quiet on the puzzle it exists for. `removal-03` passes on [PRODUCTION] for
         * the wrong reason — the turns-form race sentinel, not the pacified Wurm's board value — so
         * this column says what the term *costs* across the other 86 positions, and
         * [PRODUCTION_CANDIDATE_BOARDVALUE] is where it can actually close anything. Same shape as
         * [PRODUCTION_PATIENCE]'s column.
         */
        val PRODUCTION_PACIFIED = PRODUCTION.copy(
            id = "production-pacified",
            creatureValuation = CreatureValuation(cantAttackCostsPower = true),
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_PATIENCE] plus both halves of
         * [CreatureValuation] — the agent that stops calling one point of damage progress and stops
         * paying full price for a creature that cannot attack.
         *
         * Two flags in one candidate because they are one finding. [PRODUCTION_RACECLOCK]'s KDoc
         * itemized both, in the same paragraph, as the reason its arena win came with a trade:
         * `activate-04` and `removal-03` both regressed when the `99.0` no-attacker sentinel stopped
         * masking them, and "fix those two and the trade should become a straight gain" is the
         * prediction this profile tests. They stay separate fields so either can be reverted alone,
         * and each has its own attribution column above.
         *
         * What a player faced in a real game until [PRODUCTION_CANDIDATE_CANTRIP] replaced it in
         * [EngineAiPlayerController]. Kept unchanged as the baseline that promotion was measured
         * against.
         *
         * **The prediction held, on both halves.** On the 87-puzzle suite it takes the live pair
         * from **81/87 to 83/87**, closing exactly the two the race clock had traded, with a failing
         * set that is a **strict subset** of `production-candidate-patience`'s. And the arena, for
         * the second time in this sequence and by the largest margin yet:
         * `just arena production-candidate-patience production-candidate-boardvalue 300` measured
         * the baseline at **45.3%, CI [42.0%, 48.7%]**, 136W-164L, 300/300 completed, 0 illegal
         * actions — the whole interval below parity. This did not merely clear the "arena-neutral
         * and a puzzle ahead" bar, it cleared the arena half outright.
         *
         * The attribution columns are the other half of the evidence, and they are deliberately
         * *empty*: [PRODUCTION_DAMAGEFADES] and [PRODUCTION_PACIFIED] each leave `production`'s
         * failing set **identical**, at 74/87. Neither term can close its own puzzle on a baseline
         * that still scores the race in turns, and neither costs anything across the other 86
         * positions — which is the cheapest available evidence that what moved here is the
         * interaction the KDoc predicted rather than two independent nudges.
         *
         * If a later run comes back below parity, revert the two call sites
         * ([EngineAiPlayerController] and [AiProfileSelector]'s fallback) rather than the flags:
         * both are off for every other profile, so backing the promotion out costs nothing and
         * loses no measurement.
         */
        val PRODUCTION_CANDIDATE_BOARDVALUE = PRODUCTION_CANDIDATE_PATIENCE.copy(
            id = "production-candidate-boardvalue",
            creatureValuation = CreatureValuation(
                markedDamageFadesAtCleanup = true,
                cantAttackCostsPower = true,
            ),
        )

        /**
         * [cashCantripsInTheEndStep] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it — the same isolation [PRODUCTION_LANDSEQ] gives land
         * sequencing.
         *
         * Unlike [PRODUCTION_PATIENCE]'s and [PRODUCTION_PACIFIED]'s columns, this one *can* close
         * its puzzle here: `timing-05` needs nothing from the race clock, so it moves wherever
         * [useCardIntent] is on. Read it for `instants-06` as much as for `timing-05` — the pair is
         * the point, and a column that closes both has broken the negative control rather than
         * fixed anything.
         *
         * **Measured, and it does exactly that: 74/87 → 75/87, closing `timing-05` and nothing
         * else, with `instants-06` held.** Which makes this column the interesting one in the pair,
         * because [PRODUCTION_CANDIDATE_CANTRIP] — the same flag on the live agent — does *not*
         * close it. See there.
         */
        val PRODUCTION_CANTRIP = PRODUCTION.copy(
            id = "production-cantrip",
            cashCantripsInTheEndStep = true,
        )

        /**
         * The cantrip window on top of what is live — **84/87**, closing `timing-05`, with a
         * failing set that is a strict subset of [PRODUCTION_CANDIDATE_BOARDVALUE]'s.
         *
         * It first measured *level* with its baseline, failing the one puzzle it exists for on the
         * exact flag that closes it one column over, and the reason is worth more than the puzzle
         * was: it was the **harness**, not the agent. `PuzzleRunner` stocked every puzzle library
         * with basic lands, so the card an Opt draws was a Forest — and [landDropIsNotCardLoss],
         * live since 2026-08-08, stops counting one land held against an unused land drop. That is a
         * simplification rather than a truth about lands, and a costly one here: drawing a Forest
         * therefore read as drawing *nothing* and stepped off the
         * topdeck cliff, which priced casting the cantrip at −4.45 against passing where the same
         * agent without that one flag said −0.45. A 1.5-point window cannot cover four points of
         * measurement error, and the term looked broken.
         *
         * The `production` column was what made it findable: [PRODUCTION_CANTRIP] closes
         * `timing-05` cleanly, because a greedy agent with no land-drop accounting never sees the
         * artifact. **A term that works on the isolation column and not on the live agent is the
         * signal to go and look at the leaf**, not to argue about rollout scale or budget tiers —
         * both of which were the wrong first guesses here. `PuzzleRunner.stockLibraries` carries the
         * fix and the general rule.
         *
         * What a player faced in a real game until [PRODUCTION_CANDIDATE_COUNTERPATIENCE] replaced it
         * in [EngineAiPlayerController]. Kept unchanged as the baseline that promotion was measured
         * against.
         *
         * Promoted on the puzzle half, with the arena half returning the same **degenerate null**
         * the patience promotion did: `just arena production-candidate-boardvalue
         * production-candidate-cantrip 100` measured **50.0%, CI [50.0%, 50.0%]**, 50W-50L,
         * 100/100 completed, 0 illegal actions — and all 50 pairs at 1-1-0. Read that as "this term
         * changes the outcome of a real sealed game rarely", which is what the mechanism predicts:
         * it fires on turns where the AI holds an instant-speed cantrip and nothing better at
         * exactly the opponent's end step. A CI spanning parity is a pass by the standing bar, and
         * the evidence here is the puzzle side, at +1 with nothing traded.
         *
         * If a later run comes back below parity, revert the two call sites
         * ([EngineAiPlayerController] and [AiProfileSelector]'s fallback) rather than the flag.
         */
        val PRODUCTION_CANDIDATE_CANTRIP = PRODUCTION_CANDIDATE_BOARDVALUE.copy(
            id = "production-candidate-cantrip",
            cashCantripsInTheEndStep = true,
        )

        /**
         * [priceLandsInHandAsMana] alone on top of [PRODUCTION], so a puzzle or an arena point that
         * moves is attributable to it — the same isolation [PRODUCTION_LANDSEQ] gives land
         * sequencing.
         *
         * This is the column to read for what the *model* does, uncontaminated by
         * [landDropIsNotCardLoss], which it supersedes and which `PRODUCTION` does not carry.
         * `sequencing-02` is the verdict that should move: it is the puzzle the earmark was built
         * for, and if pricing a land honestly does not also close it, the model is wrong on the one
         * case it has to get right.
         */
        val PRODUCTION_MANALANDS = PRODUCTION.copy(
            id = "production-manalands",
            priceLandsInHandAsMana = true,
        )

        /**
         * [PRODUCTION_CANDIDATE_CANTRIP] plus [priceLandsInHandAsMana] — the agent that stops
         * pricing the land in its hand at nothing.
         *
         * Unlike the four promotions before it, this one is **expected to move the arena in one
         * direction or the other rather than sit at parity**, and should be read that way. Every
         * other term in this sequence fires on a specific shape — a pacified creature, a cantrip at
         * an end step, removal aimed at a 1/1 — and the two that returned degenerate null CIs did so
         * because those shapes are rare. This one changes what *every hand containing a land* is
         * worth, on every evaluation, including inside every rollout. A null result here would
         * itself be surprising and would mean something is not wired.
         *
         * [landDropIsNotCardLoss] stays set on this profile and is simply not read, so the diff
         * against its baseline is one flag and reverting is one line.
         */
        val PRODUCTION_CANDIDATE_MANALANDS = PRODUCTION_CANDIDATE_CANTRIP.copy(
            id = "production-candidate-manalands",
            priceLandsInHandAsMana = true,
        )

        /**
         * [holdCountersForBetterSpells] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it — the same isolation [PRODUCTION_LANDSEQ] gives land
         * sequencing.
         *
         * This column **cannot** close its own puzzle, and that is not a failure of the term:
         * `production` already passes `respond-02`, because a greedy agent declines to counter there
         * for reasons of its own. So what it measures is what the term *costs* across the other 86
         * positions — the same reading [PRODUCTION_PATIENCE]'s and [PRODUCTION_PACIFIED]'s columns
         * get, and for the same structural reason.
         *
         * **Measured: 74/87, a failing set identical to [PRODUCTION]'s.** Nothing moves in either
         * direction, which is the cheapest available evidence that what closes `respond-02` one
         * column over is this term rather than an interaction, and that no counter the AI *should*
         * make was traded for it.
         */
        val PRODUCTION_COUNTERPATIENCE = PRODUCTION.copy(
            id = "production-counterpatience",
            holdCountersForBetterSpells = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_CANTRIP] plus [holdCountersForBetterSpells]
         * — the agent that stops spending its only counterspell on the first two-drop it sees.
         *
         * Stacked on [PRODUCTION_CANDIDATE_CANTRIP] rather than [PRODUCTION_CANDIDATE_MANALANDS]
         * because the cantrip window is what [EngineAiPlayerController] actually points at; the
         * manalands model is a live experiment whose arena half is still out, and a candidate has to
         * be one flag away from what players face. If manalands promotes first, re-base this on it
         * and re-measure rather than assuming the two are independent.
         *
         * **Measured: 84/87 → 85/87**, closing `respond-02`, with a failing set that is a strict
         * subset of [PRODUCTION_CANDIDATE_CANTRIP]'s — what is left is `respond-05` and `timing-03`,
         * both of which need a horizon past the current resolution rather than a term.
         *
         * The negative controls are the rest of its own category — `respond-01` (Serra Angel), `-03`
         * (Wrath), `-04` (Murder aimed at our Angel) and `-06` (Negate over Essence Scatter) — all
         * four cast by a *tapped out* opponent, so
         * [com.wingedsheep.ai.engine.knowledge.CounterPatience]'s bar is zero there by construction.
         * All four hold, as do the other 82 positions.
         *
         * **What a player faces in a real game today** — see [EngineAiPlayerController].
         *
         * Promoted on the puzzle half, with the arena half returning the same **degenerate null**
         * the patience and cantrip promotions did: `just arena production-candidate-cantrip
         * production-candidate-counterpatience 100` measured **50.0%, CI [50.0%, 50.0%]**,
         * 49W-49L-2D, 0 illegal actions — and every scored pair at 1-1-0. Read that as "this term
         * changes the outcome of a real sealed game rarely", which is what the mechanism predicts:
         * it fires only on a turn where the AI holds a counterspell against a spell whose caster
         * still has mana up. A CI spanning parity is a pass by the standing bar, and the evidence
         * here is the puzzle side, at +1 with nothing traded.
         *
         * The two unfinished games were a pre-existing `NoClassDefFoundError` on a test worker's
         * classpath (`sdk.scripting.RetainUnspentColoredMana`, a class that exists in source and in
         * `mtg-sdk/build`), not an agent fault, and they hit both sides of the same pair.
         *
         * If a later run comes back below parity, revert the two call sites
         * ([EngineAiPlayerController] and [AiProfileSelector]'s fallback) rather than the flag: it
         * is off for every other profile, so backing the promotion out costs nothing and loses no
         * measurement.
         */
        val PRODUCTION_CANDIDATE_COUNTERPATIENCE = PRODUCTION_CANDIDATE_CANTRIP.copy(
            id = "production-candidate-counterpatience",
            holdCountersForBetterSpells = true,
        )

        /**
         * [holdFlashPermanentsForAmbush] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it — the same isolation [PRODUCTION_COUNTERPATIENCE] gives
         * counter patience.
         *
         * Unlike that column this one *can* close its own puzzle, because the mistake is not one a
         * greedy agent declines to make for reasons of its own: `production` already has
         * [useCardIntent], so it reads Restoration Angel's flash, types it `Speed.INSTANT`, finds no
         * branch that claims it, and jams it in the main phase exactly as the live agent does.
         *
         * **Measured: 78/92 → 79/92**, closing `instants-09` and nothing else, with every other
         * category byte-identical to [PRODUCTION]'s. A term that fires on one narrow shape and
         * costs nothing across the other 91 positions is what the isolation column is for.
         */
        val PRODUCTION_AMBUSH = PRODUCTION.copy(
            id = "production-ambush",
            holdFlashPermanentsForAmbush = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_COUNTERPATIENCE] plus
         * [holdFlashPermanentsForAmbush] — the agent that stops dumping its flash creatures into its
         * own main phase.
         *
         * Stacked on [PRODUCTION_CANDIDATE_COUNTERPATIENCE] because that is what
         * [com.wingedsheep.server.game.EngineAiPlayerController] actually points at, and a candidate
         * has to be one flag away from what players face — the same rule
         * [PRODUCTION_CANDIDATE_COUNTERPATIENCE]'s own KDoc states about not stacking on the
         * manalands experiment.
         *
         * **Measured: 89/92 → 90/92**, closing `instants-09` with a failing set that is a strict
         * subset of [PRODUCTION_CANDIDATE_COUNTERPATIENCE]'s — what is left is `respond-05` and
         * `timing-03`, the same two that need a horizon rather than a term. The `instants` category
         * goes 12/13 → **13/13** and every other category is unchanged.
         *
         * Its controls are the four positions added alongside it, all of which pass on
         * [PRODUCTION] already and still pass here: `instants-10` (the ambush itself, at their
         * declare-attackers), `-11` (flash *and* haste), `-12` (an ETB that taps a blocker before
         * we attack) and `-13` (past the patience horizon). `instants-10` is not a fix — it passes
         * everywhere — and it is in the suite for the reason `HoldingInstantsPuzzles`' own header
         * gives: a category of "don't cast" positions scores 100% for an agent that never casts
         * anything, so the half that says *do* has to be asserted too.
         *
         * The arena half returned the sequence's **fourth degenerate null**: `just arena
         * production-candidate-counterpatience production-candidate-ambush 100` measured **50.0%,
         * CI [50.0%, 50.0%]**, 100/100 completed, 0 illegal actions, every scored pair 1-1-0. Read
         * that as "this term changes the outcome of a real sealed game rarely", which is what the
         * mechanism predicts — it fires only on a turn where the AI holds a flash permanent with
         * the ambush window still ahead. A CI spanning parity is a pass by the standing bar, and
         * the evidence is the puzzle side, at +1 with nothing traded.
         *
         * If a later arena run comes back below parity, revert the two call sites
         * (`EngineAiPlayerController` and [AiProfileSelector]'s fallback) rather than the flag: it is
         * off for every other profile, so backing the promotion out costs nothing and loses no
         * measurement.
         */
        val PRODUCTION_CANDIDATE_AMBUSH = PRODUCTION_CANDIDATE_COUNTERPATIENCE.copy(
            id = "production-candidate-ambush",
            holdFlashPermanentsForAmbush = true,
        )

        /**
         * [holdExpiringGrantsForCombat] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it — the same isolation [PRODUCTION_AMBUSH] gives the
         * ambush window.
         *
         * Like that column this one can close its own puzzle unaided: `production` already has
         * [useCardIntent], so the catalog resolves Olivia's Dragoon's ability and the flag is the
         * only thing standing between the position and a floor.
         *
         * **Measured: 81/96 → 82/96**, closing `instants-14` and nothing else, with every other
         * category byte-identical to [PRODUCTION]'s. A term that fires on one narrow shape and
         * costs nothing across the other 95 positions is what the isolation column is for.
         */
        val PRODUCTION_EXPIRING = PRODUCTION.copy(
            id = "production-expiring",
            holdExpiringGrantsForCombat = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_COUNTERPATIENCE] plus
         * [holdExpiringGrantsForCombat] — the agent that stops pitching cards for keywords it has
         * nothing to spend them on.
         *
         * Stacked on [PRODUCTION_CANDIDATE_COUNTERPATIENCE], **not** on
         * [PRODUCTION_CANDIDATE_AMBUSH], because that is what
         * [com.wingedsheep.ai.engine.EngineAiPlayerController] actually points at: the ambush
         * flag's own promotion never reached the call site, so stacking on it would make this
         * candidate two flags from what players face and its arena number unattributable.
         *
         * Its controls are the two positions added alongside it: `instants-15` (the same ability at
         * the opponent's declare-attackers, where the window is released and the block it buys is
         * real) and `instants-16` (the same board at a full hand, where the discard is the card
         * cleanup was taking anyway). A term that only ever says "don't" scores well on a suite by
         * never playing, which is why the half that says *do* is asserted too.
         *
         * **Measured: 91/96 → 92/96**, closing `instants-14` with a failing set that is a strict
         * subset of [PRODUCTION_CANDIDATE_COUNTERPATIENCE]'s. The `instants` category goes 14/17 →
         * 15/17 and every other category is byte-identical; what is left is `instants-08`,
         * `respond-05`, `timing-01` and `timing-03`, none of which this term is about.
         *
         * The arena half returned another **degenerate null**: `just arena
         * production-candidate-counterpatience production-candidate-expiring 100` measured **50.0%,
         * CI [50.0%, 50.0%]**, 100/100 completed, 0 illegal actions, and *every* scored pair 1-1-0.
         * Pairs that identical mean the term did not fire at all across 100 BLB sealed games, which
         * the mechanism predicts — it needs a permanent with an instant-speed ability whose only
         * payoff expires, held in a pre-combat window, and BLB is not a set full of those. Read the
         * arena as **no regression**, not as evidence of gain; the evidence for the gain is the
         * puzzle side and the game it was taken from, which was neither BLB nor sealed.
         *
         * If a later arena run comes back below parity, revert the one call site
         * ([com.wingedsheep.ai.engine.EngineAiPlayerController]) rather than the flag: it is off for
         * every other profile, so backing the promotion out costs nothing and loses no measurement.
         */
        val PRODUCTION_CANDIDATE_EXPIRING = PRODUCTION_CANDIDATE_COUNTERPATIENCE.copy(
            id = "production-candidate-expiring",
            holdExpiringGrantsForCombat = true,
        )

        /** [permanentCastIsNotCardLoss] alone on top of [PRODUCTION], for attribution. */
        val PRODUCTION_DEPLOY = PRODUCTION.copy(
            id = "production-deploy",
            permanentCastIsNotCardLoss = true,
        )

        /**
         * [PRODUCTION_CANDIDATE_EXPIRING] plus [permanentCastIsNotCardLoss] — the agent that stops
         * hoarding castable permanents. Stacked on the profile the 2026-10-09 game logs were taken
         * with, so the cited misplays are measured against the agent that made them.
         */
        val PRODUCTION_CANDIDATE_DEPLOY = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-deploy",
            permanentCastIsNotCardLoss = true,
        )

        /**
         * [castabilityAwareCardSelection] alone on top of [PRODUCTION], so a puzzle or an arena
         * point that moves is attributable to it.
         */
        val PRODUCTION_CARDSELECT = PRODUCTION.copy(
            id = "production-cardselect",
            castabilityAwareCardSelection = true,
        )

        /**
         * [PRODUCTION_CANDIDATE_EXPIRING] — the candidate [LIVE] starts from — plus
         * [castabilityAwareCardSelection]. The promotion gate is `just arena
         * production-candidate-expiring production-candidate-cardselect 300`.
         */
        val PRODUCTION_CANDIDATE_CARDSELECT = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-cardselect",
            castabilityAwareCardSelection = true,
        )

        /**
         * [refuseUnspendableGrants] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it and nothing else.
         *
         * Without [holdExpiringGrantsForCombat] this column carries only the "nothing can spend
         * it" floors — a grant no fight left this turn can use, one already in force, a trick on a
         * creature out of combat. The deferral floor, and the long-game release this flag removes
         * from it, belong to [PRODUCTION_CANDIDATE_NOOP].
         */
        val PRODUCTION_NOOP = PRODUCTION.copy(
            id = "production-noop",
            refuseUnspendableGrants = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_EXPIRING] plus [refuseUnspendableGrants]
         * — the agent that stops buying end-of-turn payoffs it has nothing to spend on. Stacked on
         * the profile the 24-game log review was taken from, so the positions it cites are the
         * ones this changes.
         */
        val PRODUCTION_CANDIDATE_NOOP = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-noop",
            refuseUnspendableGrants = true,
        )

        /**
         * [chumpOnlyWhenInDanger] alone on top of [PRODUCTION], so a puzzle or an arena point that
         * moves is attributable to it.
         */
        val PRODUCTION_CHUMPGATE = PRODUCTION.copy(
            id = "production-chumpgate",
            chumpOnlyWhenInDanger = true,
        )

        /**
         * [chumpOnlyWhenInDanger] on top of [PRODUCTION_CANDIDATE_EXPIRING] — the last pinned
         * candidate [LIVE] builds on, so the arena gate `just arena production-candidate-expiring
         * production-candidate-chumpgate` measures this flag and nothing else. [LIVE] ships it.
         */
        val PRODUCTION_CANDIDATE_CHUMPGATE = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-chumpgate",
            chumpOnlyWhenInDanger = true,
        )

        /**
         * The attack-decision fixes from the 2026-10-09 AI game-log review, on top of [PRODUCTION],
         * so a puzzle or an arena point can be attributed to them alone.
         *
         * Two flags, one per half of the attack decision that review found broken:
         * - [priceCrackBackAsLife] — the hold-back half. The flat −3.0 only fires at exactly lethal,
         *   and the evaluator prices the attack's own damage on a curve that is steepest at low
         *   life, so near the end of a race it routinely outbid the crack-back: at 8 life against
         *   15 power, swinging two creatures at an opponent on 7 bought 8.0 of life value against a
         *   3.0 penalty, and the AI died to the swing back (game 17 turn 21). Charged as life the
         *   same crack-back costs `lifeValue(8) − lifeValue(dead)` = 118. Also game 13 turn 17
         *   (sent its only blocker at 9 life, took 6) and game 23 turns 15–17 (a deathtouch 1/1
         *   attacking for one into a 7/7 it was the only answer to). Existing flag, never measured
         *   in the arena: its puzzle target was `race-03`, which it does not close.
         *   [crackBackWithoutChumps] rides with it: priced as life, a crack-back that lets every
         *   creature at home chump for free made holding a 2/2 back look better than attacking past
         *   a tapped 3/3 (`race-06`).
         * - [evasionAfterAttacking] — the free-attack half; see its KDoc.
         */
        val PRODUCTION_ATTACKS = PRODUCTION.copy(
            id = "production-attacks",
            priceCrackBackAsLife = true,
            crackBackWithoutChumps = true,
            evasionAfterAttacking = true,
        )

        /**
         * The attribution candidate: [PRODUCTION_CANDIDATE_EXPIRING], the last pinned baseline, plus
         * the attack flags of [PRODUCTION_ATTACKS]. All three also ship in [LIVE].
         */
        val PRODUCTION_CANDIDATE_ATTACKS = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-attacks",
            priceCrackBackAsLife = true,
            crackBackWithoutChumps = true,
            evasionAfterAttacking = true,
        )

        /**
         * **What real players face.** [EngineAiPlayerController] builds this and nothing else.
         *
         * A named, stable home for the live configuration so a fix can ship by turning its flag on
         * *here*, in the same PR that adds it, instead of every promotion re-pointing the
         * controller at a fresh `production-candidate-*` id (which serialises parallel work behind
         * one line). The superseded candidates stay put as the baselines they were measured
         * against; `just arena production-candidate-expiring live 300` prices everything stacked
         * on top of the last pinned candidate.
         *
         * Add one line per shipped flag, with the arena interval that justified it in the KDoc of
         * the flag itself.
         */
        val LIVE = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "live",
            castabilityAwareCardSelection = true,
            informedChoiceDecisions = true,
            refuseUnspendableGrants = true,
            targetPolarityFromEffect = true,
            chumpOnlyWhenInDanger = true,
            permanentCastIsNotCardLoss = true,
            priceCrackBackAsLife = true,
            crackBackWithoutChumps = true,
            evasionAfterAttacking = true,
            pendingLandDropIsMana = true,
            selectionCountsByValue = true,
            fillUpToMaxTargets = true,
            refuseDeadSearches = true,
        )

        /**
         * [refuseDeadSearches] alone on top of [PRODUCTION], so a puzzle or an arena point that
         * moves is attributable to it.
         */
        val PRODUCTION_DEADSEARCH = PRODUCTION.copy(
            id = "production-deadsearch",
            refuseDeadSearches = true,
        )

        /**
         * [refuseDeadSearches] on top of [PRODUCTION_CANDIDATE_EXPIRING] — the profile the cited
         * game log was played on — so `just arena production-candidate-expiring
         * production-candidate-deadsearch 300` measures this flag and nothing else. [LIVE] ships it.
         */
        val PRODUCTION_CANDIDATE_DEADSEARCH = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-deadsearch",
            refuseDeadSearches = true,
        )

        /**
         * [fillUpToMaxTargets] alone on top of [PRODUCTION], so a puzzle or an arena point that
         * moves is attributable to it.
         */
        val PRODUCTION_UPTO = PRODUCTION.copy(
            id = "production-upto",
            fillUpToMaxTargets = true,
        )

        /**
         * [fillUpToMaxTargets] on top of [PRODUCTION_CANDIDATE_EXPIRING] — the last pinned
         * candidate [LIVE] builds on, so `just arena production-candidate-expiring
         * production-candidate-upto 300` measures this flag and nothing else. [LIVE] ships it.
         */
        val PRODUCTION_CANDIDATE_UPTO = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-upto",
            fillUpToMaxTargets = true,
        )

        /**
         * [selectionCountsByValue] alone on top of [PRODUCTION], so a puzzle or an arena point that
         * moves is attributable to it.
         */
        val PRODUCTION_SELECTCOUNT = PRODUCTION.copy(
            id = "production-selectcount",
            selectionCountsByValue = true,
        )

        /**
         * [PRODUCTION_CANDIDATE_EXPIRING] — the profile the cited game logs were taken with — plus
         * [selectionCountsByValue].
         */
        val PRODUCTION_CANDIDATE_SELECTCOUNT = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-selectcount",
            selectionCountsByValue = true,
        )

        /**
         * [informedChoiceDecisions] alone on top of [PRODUCTION], so a puzzle or an arena point
         * that moves is attributable to it.
         */
        val PRODUCTION_CHOICES = PRODUCTION.copy(
            id = "production-choices",
            informedChoiceDecisions = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_EXPIRING] plus [informedChoiceDecisions].
         * Stacked on the expiring candidate because that is the live profile the 2026-10-09 log
         * review played; the combining PR decides what it finally stacks on.
         */
        val PRODUCTION_CANDIDATE_CHOICES = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-choices",
            informedChoiceDecisions = true,
        )

        /**
         * [targetPolarityFromEffect] alone on top of [PRODUCTION], so a puzzle or arena point that
         * moves is attributable to it.
         */
        val PRODUCTION_POLARITY = PRODUCTION.copy(
            id = "production-polarity",
            targetPolarityFromEffect = true,
        )

        /**
         * The promotion candidate: [PRODUCTION_CANDIDATE_EXPIRING] — the
         * candidate [LIVE] starts from — plus
         * [targetPolarityFromEffect].
         */
        val PRODUCTION_CANDIDATE_POLARITY = PRODUCTION_CANDIDATE_EXPIRING.copy(
            id = "production-candidate-polarity",
            targetPolarityFromEffect = true,
        )

        /**
         * Everything Phase 4 added, on top of [LEGACY_V0]: the meaningful-action filter and the
         * four-tier decision budget at its nominal sizes. The arena agent `v0-phase4`.
         */
        val PHASE4 = AiProfile(
            id = "v0-phase4",
            useMeaningfulFilter = true,
            budgetPolicy = TieredBudgetPolicy(),
        )

        /**
         * Phase 6's card knowledge alone, on top of [LEGACY_V0]. The arena agent `v0-intent`:
         * `just arena v0 v0-intent 1000` is the phase's merge gate, and isolating it from Phase 4
         * is what makes that number attributable.
         */
        val PHASE6 = AiProfile(
            id = "v0-intent",
            useCardIntent = true,
        )

        /** Everything Phases 4 and 6 add — what the plan proposes to ship. */
        val PHASE4_PHASE6 = AiProfile(
            id = "v0-phase4-intent",
            useMeaningfulFilter = true,
            budgetPolicy = TieredBudgetPolicy(),
            useCardIntent = true,
        )

        /**
         * Phase 7's rollout evaluator alone, on top of [LEGACY_V0]. The arena agent `v0-rollout`:
         * `just arena v0 v0-rollout 1000` is the phase's merge gate, and isolating it from Phases 4
         * and 6 is what makes that number attributable to the rollouts.
         */
        val PHASE7 = AiProfile(
            id = "v0-rollout",
            rollouts = RolloutSettings.DEFAULT,
        )

        /** Phase 7 search over a fair, sampled hidden-information state. */
        val PHASE8 = AiProfile(
            id = "v0-rollout-determinized",
            rollouts = RolloutSettings.DEFAULT,
            determinizeHiddenInformation = true,
        )

        /** Everything Phases 4, 6 and 7 add — what the plan proposes to ship. */
        val PHASE4_PHASE6_PHASE7 = AiProfile(
            id = "v0-phase4-intent-rollout",
            useMeaningfulFilter = true,
            budgetPolicy = TieredBudgetPolicy(),
            useCardIntent = true,
            rollouts = RolloutSettings.DEFAULT,
        )

        val ECL_APPRENTICE = PRODUCTION.copy(
            id = "ecl-apprentice",
            evalWeightsId = "ecl-apprentice",
            restrictedToSet = "ECL",
        )

        val ECL_OVERLAY = PRODUCTION.copy(
            id = "ecl-overlay",
            evalWeightsId = "ecl-overlay",
            restrictedToSet = "ECL",
        )
    }
}

/** The only automatic promotion seam: a set-scoped profile cannot leak into another format. */
object AiProfileSelector {
    fun select(
        setCode: String?,
        requested: AiProfile?,
        fallback: AiProfile = AiProfile.PRODUCTION_CANDIDATE_COUNTERPATIENCE,
    ): AiProfile {
        if (requested == null) return fallback
        val restriction = requested.restrictedToSet ?: return requested
        return if (setCode?.uppercase() == restriction.uppercase()) requested else fallback
    }
}
