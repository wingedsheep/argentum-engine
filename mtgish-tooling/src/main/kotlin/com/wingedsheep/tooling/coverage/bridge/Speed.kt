package com.wingedsheep.tooling.coverage.bridge

/**
 * Speed (Aetherdrift, CR 702.178–702.179): "Start your engines!", "Max speed — [Ability]", and the
 * numbers/filters that read a player's speed.
 *
 * Seven IR tags across the corpus. Two make the mechanic work and are mapped here; the two aggregate
 * shapes stay unmapped so the cards that need them keep blocking until the engine grows a
 * max-over-players amount.
 *
 * `StartYourEngines` and `MaxSpeed` are `supported`, not `keyword`, for the Station reason: the engine
 * *does* have `Keyword.START_YOUR_ENGINES` / `Keyword.MAX_SPEED`, but the capability isn't the enum
 * member — it's the CR 704.5z state-based action and the condition gate the `startYourEngines()` /
 * `maxSpeed { }` builders wire. Registering them as `keyword` would score them off an enum whose
 * presence alone proves nothing about whether the rules engine acts on it.
 */
fun BridgeBuilder.speed() {
    // `_Rule: StartYourEngines` — the keyword. Argentum starts the controller's speed via the CR 704.5z
    // state-based action (StartYourEnginesCheck), so the card only needs the keyword tag.
    supported(
        "StartYourEngines",
        "keyword ability: Start your engines! (CR 702.179a) -> startYourEngines(); speed becomes 1 via the CR 704.5z state-based action"
    )
    // `_Rule: MaxSpeed` wrapping a nested `_Rule` — "Max speed — [Ability]" (CR 702.178a). The gate is
    // Conditions.YouHaveMaxSpeed applied with each ability kind's own vocabulary (ConditionalStaticAbility /
    // ActivationRestriction.OnlyIfCondition / triggerCondition), so the capability is real for static,
    // activated and triggered payloads. The emitter renders the static-keyword and adjust-P/T shapes and
    // declines the rest to SCAFFOLD; a *replacement-effect* payload (Vnwxt, Far Fortune) is not yet
    // expressible at all — see the note in card-sdk-language-reference.md.
    supported(
        "MaxSpeed",
        "keyword ability: Max speed — [Ability] (CR 702.178a) -> maxSpeed { } gating the nested ability on Conditions.YouHaveMaxSpeed"
    )
    // `_GameNumber: SpeedOfPlayer` — "where X is your speed" (Point the Way, The Speed Demon, Samut,
    // Momentum Breaker). DynamicAmount.Speed(player) is outside the scanned effects/ dir, so composed.
    composed(
        "SpeedOfPlayer",
        "a player's speed 0-4 (CR 702.179f: no speed reads as 0) -> DynamicAmount.Speed(player)"
    )
    // `_Players: HasMaxSpeed` / `DoesntHaveMaxSpeed` — the player filters behind "unless you have max
    // speed" (Hazoret, Godseeker) and "each player who doesn't have max speed" (Outpace Oblivion).
    // Both are Compare over DynamicAmount.Speed, the negative one wrapped in Conditions.Not.
    composed(
        "HasMaxSpeed",
        "player filter: speed is exactly 4 (CR 702.179e) -> Conditions.HasMaxSpeed(player)",
        composes = listOf("Speed")
    )
    composed(
        "DoesntHaveMaxSpeed",
        "player filter: speed is not 4 -> Conditions.Not(HasMaxSpeed(player))",
        composes = listOf("Speed")
    )
    // `_Action: ReducePlayersSpeed` — Spikeshell Harrier's "reduce that opponent's speed by 1. This
    // effect can't reduce their speed below 1." Shares ChangeSpeedEffect with the increasing half; the
    // card's floor is the effect's `minimum`.
    composed(
        "ReducePlayersSpeed",
        "reduce a player's speed, with the card's own floor -> Effects.ReduceSpeed(amount, target, minimum)",
        composes = listOf("ChangeSpeed")
    )

    // DELIBERATELY UNMAPPED (both blocking, both needed only by Spikeshell Harrier):
    //  - `_Players: SpeedIs` — a speed *comparison* against an arbitrary GameNumber rather than the
    //    fixed 4 of HasMaxSpeed. Expressible as Compare(Speed(player), op, <amount>) in principle, but
    //    its only corpus use compares against TheHighestSpeedAmongPlayers, so mapping it alone would
    //    score the card coverable while it still can't be authored.
    //  - `_GameNumber: TheHighestSpeedAmongPlayers` — max of Speed over a player set. Argentum has no
    //    max-over-players DynamicAmount (Max only combines two amounts), so this genuinely blocks. The
    //    unlock is a player-aggregate amount, which is its own feature, not part of speed.
}
