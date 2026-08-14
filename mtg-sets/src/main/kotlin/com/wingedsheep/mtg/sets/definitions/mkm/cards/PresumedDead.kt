package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Presumed Dead — Murders at Karlov Manor #100
 * {1}{B} · Instant · Uncommon
 *
 * Until end of turn, target creature gets +2/+0 and gains "When this creature dies, return it to
 * the battlefield under its owner's control and suspect it." (A suspected creature has menace and
 * can't block.)
 *
 * The Murders-flavoured [com.wingedsheep.mtg.sets.definitions.vow.cards.UndyingMalice] — a combat
 * trick that also makes the trade a non-trade. The returned creature comes back with menace and
 * unable to block, which is upside on the turn you swing and a real cost on defence: a blocker you
 * saved this way cannot block again.
 *
 * Two clauses, one duration. Both the +2/+0 and the granted ability last until end of turn, so a
 * creature that survives the turn keeps nothing — this is not a permanent recursion effect. The
 * grant is [GrantTriggeredAbilityEffect] with `Duration.EndOfTurn` and the same self-bound dies
 * trigger (battlefield → graveyard) that Undying Malice uses.
 *
 * The granted body is a `graveyard → battlefield` move gated with `fromZone = Zone.GRAVEYARD`, so
 * it safely no-ops if something else already moved the card out of the graveyard in response.
 * `MoveToZoneEffect` returns a card under its **owner's** control by default, which is exactly what
 * "under its owner's control" asks for — stealing a creature, pumping it and letting it die returns
 * it to its owner, not to you.
 *
 * The return keeps the same entity id, so the follow-up [Effects.Suspect] on `EffectTarget.Self`
 * lands on the returned permanent rather than on last-known information. Note the suspect is
 * `Duration.Permanent` (the default): per the printed rulings the creature stays suspected past
 * this turn's cleanup, even though the grant that caused it has expired. Modelling the suspect as
 * an end-of-turn effect would incorrectly un-suspect it.
 */
val PresumedDead = card("Presumed Dead") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +2/+0 and gains \"When this creature " +
        "dies, return it to the battlefield under its owner's control and suspect it.\" " +
        "(A suspected creature has menace and can't block.)"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, creature, Duration.EndOfTurn),
            GrantTriggeredAbilityEffect(
                ability = TriggeredAbility.create(
                    trigger = Triggers.Dies.event,
                    binding = Triggers.Dies.binding,
                    effect = Effects.Composite(
                        Effects.Move(
                            target = EffectTarget.Self,
                            destination = Zone.BATTLEFIELD,
                            fromZone = Zone.GRAVEYARD
                        ),
                        Effects.Suspect(EffectTarget.Self),
                    ),
                    descriptionOverride = "When this creature dies, return it to the battlefield " +
                        "under its owner's control and suspect it."
                ),
                target = creature,
                duration = Duration.EndOfTurn
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Matt Forsyth"
        flavorText = "\"Corpses are pulled from the undercity canals every day. They'll assume " +
            "one's mine.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dd64e5c-ea0b-4ea0-aba3-88e7e96ac7ba.jpg?1783912895"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until " +
                "it leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
    }
}
