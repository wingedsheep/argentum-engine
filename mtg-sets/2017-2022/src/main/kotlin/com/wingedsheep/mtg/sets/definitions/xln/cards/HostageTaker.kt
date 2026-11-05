package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hostage Taker
 * {2}{U}{B}
 * Creature — Human Pirate
 * 2/3
 *
 * When this creature enters, exile another target creature or artifact until this creature leaves
 * the battlefield. You may cast that card for as long as it remains exiled, and mana of any type
 * can be spent to cast that spell.
 *
 * The Banisher Priest shape — [Effects.ExileUntilLeaves] links the exiled card to Hostage Taker
 * (and does nothing if Hostage Taker already left before the trigger resolved, per the ruling),
 * and the leaves trigger returns whatever is *still* in the linked pile under its owner's control.
 * The cast permission is then granted from that same linked pile (Thief of Sanity's
 * `Permanent` + `withAnyManaType` grant): it goes to the trigger's controller at resolution, so it
 * isn't tied to who controls Hostage Taker later. Casting the card moves it off the exile pile, so
 * the leaves trigger no longer finds it and it stays with its caster. `nonLandOnly` carries the
 * "cast" wording for an exiled artifact land. A token exiled this way ceases to exist, so the
 * linked pile is empty and nothing is granted.
 */
val HostageTaker = card("Hostage Taker") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, exile another target creature or artifact until this " +
        "creature leaves the battlefield. You may cast that card for as long as it remains exiled, " +
        "and mana of any type can be spent to cast that spell."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val hostage = target(TargetFilter.CreatureOrArtifact.other())
        effect = Effects.ExileUntilLeaves(hostage) then Effects.Pipeline {
            val taken = gather(CardSource.FromLinkedExile())
            run(Effects.GrantMayPlayFromExile(
                taken,
                expiry = MayPlayExpiry.Permanent,
                withAnyManaType = true,
                nonLandOnly = true,
            ))
        }
        description = "When this creature enters, exile another target creature or artifact until " +
            "this creature leaves the battlefield. You may cast that card for as long as it remains " +
            "exiled, and mana of any type can be spent to cast that spell."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8dc67a0-05d9-463e-9794-30a7e951c240.jpg?1783935713"

        ruling("2017-09-29", "If Hostage Taker leaves the battlefield before its triggered ability " +
            "resolves, the target permanent won't be exiled.")
        ruling("2017-09-29", "If a token is exiled this way, it will cease to exist and won't return " +
            "to the battlefield. You can't cast it.")
        ruling("2017-09-29", "Once you begin to cast the exiled card, it's considered a new object. " +
            "You'll control that spell and the permanent that spell becomes even if Hostage Taker " +
            "leaves the battlefield.")
        ruling("2017-09-29", "If it's still in exile, the exiled card returns to the battlefield " +
            "immediately after Hostage Taker leaves the battlefield. Nothing happens between the two " +
            "events, including state-based actions.")
    }
}
