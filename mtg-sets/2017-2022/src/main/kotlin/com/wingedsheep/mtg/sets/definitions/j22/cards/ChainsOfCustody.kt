package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chains of Custody
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature you control
 * When this Aura enters, exile target nonland permanent an opponent controls until this Aura
 * leaves the battlefield.
 * Enchanted creature has ward {2}.
 *
 * The Banishing Light pair: the ETB links the exiled permanent to this Aura via
 * [Effects.ExileUntilLeaves], and the leave trigger returns it with
 * [Effects.ReturnLinkedExileUnderOwnersControl]. If the Aura leaves before the ETB resolves, the
 * linked-exile gate exiles nothing (ruling 2022-12-02). Ward {2} is granted to the enchanted
 * creature as a static [GrantWard].
 */
val ChainsOfCustody = card("Chains of Custody") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When this Aura enters, exile target nonland permanent an opponent controls until this Aura " +
        "leaves the battlefield.\n" +
        "Enchanted creature has ward {2}. (Whenever it becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {2}.)"

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    staticAbility { ability = GrantWard(WardCost.Mana("{2}")) }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Bruno Biazotto"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94a1a840-bcdc-4d6f-a28d-9805578473f6.jpg?1783919196"
        ruling(
            "2022-12-02",
            "Auras attached to the exiled permanent will be put into their owners' graveyards. Any Equipment will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist. When the card returns to the battlefield, it will be a new object with no connection to the card that was exiled."
        )
        ruling(
            "2022-12-02",
            "If a token is exiled this way, it will cease to exist and won't return to the battlefield."
        )
        ruling(
            "2022-12-02",
            "If Chains of Custody leaves the battlefield before its second ability resolves, the target permanent won't be exiled."
        )
    }
}
