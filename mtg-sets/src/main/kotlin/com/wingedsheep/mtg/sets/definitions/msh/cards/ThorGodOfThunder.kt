package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Thor, God of Thunder — Marvel Super Heroes #156
 * {3}{R}{R} · Legendary Creature — God Warrior Hero · 5/5
 *
 * Flying
 * When Thor enters, exile target Equipment, instant, or sorcery card from your graveyard. Until
 * the end of your next turn, you may play that card.
 * Whenever you cast a noncreature spell, Thor deals damage equal to that spell's mana value to any
 * target.
 *
 * Distinct from Thor Odinson (MSH #234) — different card, different name.
 *
 * Implementation notes:
 *  - The ETB is the targeted-impulse pipeline (Quintorius Kand's −6): [GatherCardsEffect] over
 *    [CardSource.ChosenTargets] references the chosen graveyard card, [MoveCollectionEffect] exiles
 *    it and re-stores the moved entities, and [Effects.GrantMayPlayFromExile] grants the play
 *    permission on that post-move collection. Gathering the *target* rather than a zone scan keeps
 *    the exile and the permission pointing at the same card even if the graveyard changes in
 *    between; if the target is gone on resolution the ability fizzles (CR 608.2b) and nothing is
 *    exiled.
 *  - "Until the end of your next turn" is [MayPlayExpiry.UntilEndOfNextTurn]
 *    (`UntilControllerStep(CLEANUP, includeCurrentTurn = false)`) — never the current turn's
 *    cleanup, even when Thor enters on your own turn.
 *  - "Equipment, instant, or sorcery card" is one homogeneous filter union: Equipment is always an
 *    artifact carrying the subtype, so the subtype alone identifies it (the CreatureOrVehicle
 *    idiom). `ownedByYou()` scopes it to your graveyard, where cards have owners rather than
 *    controllers.
 *  - "that spell's mana value" is [DynamicAmounts.triggeringManaValue] — the printed mana value of
 *    the spell that fired the trigger (CR 202.3), the same slot Alchemist's Talent reads. Thor's
 *    trigger goes on the stack above that spell, so it resolves first; a spell countered in
 *    response still leaves the trigger to resolve for its mana value.
 */
val ThorGodOfThunder = card("Thor, God of Thunder") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — God Warrior Hero"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Thor enters, exile target Equipment, instant, or sorcery card from your graveyard. " +
        "Until the end of your next turn, you may play that card.\n" +
        "Whenever you cast a noncreature spell, Thor deals damage equal to that spell's mana " +
        "value to any target."

    keywords(Keyword.FLYING)

    // "When Thor enters, exile target Equipment, instant, or sorcery card from your graveyard.
    //  Until the end of your next turn, you may play that card."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "target Equipment, instant, or sorcery card from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = (
                        GameObjectFilter.InstantOrSorcery or
                            GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT)
                        ).ownedByYou(),
                    zone = Zone.GRAVEYARD,
                )
            )
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "thorTargeted"),
                MoveCollectionEffect(
                    from = "thorTargeted",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    storeMovedAs = "thorExiled",
                ),
                Effects.GrantMayPlayFromExile("thorExiled", MayPlayExpiry.UntilEndOfNextTurn),
            )
        )
        description = "When Thor enters, exile target Equipment, instant, or sorcery card from " +
            "your graveyard. Until the end of your next turn, you may play that card."
    }

    // "Whenever you cast a noncreature spell, Thor deals damage equal to that spell's mana value
    //  to any target."
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        val victim = target("any target", Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.triggeringManaValue(), victim)
        description = "Whenever you cast a noncreature spell, Thor deals damage equal to that " +
            "spell's mana value to any target."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "156"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cddd314c-c271-475a-b076-01a8599c8015.jpg?1783902923"
    }
}
